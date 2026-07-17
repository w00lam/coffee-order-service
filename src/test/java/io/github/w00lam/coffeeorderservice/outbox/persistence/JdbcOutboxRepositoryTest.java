package io.github.w00lam.coffeeorderservice.outbox.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.sql.Timestamp;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@JdbcTest
@Import(JdbcOutboxRepository.class)
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class JdbcOutboxRepositoryTest extends PostgreSqlIntegrationTest {
	private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private JdbcOutboxRepository repository;

	@BeforeEach
	void setUp() {
		clearData();
		jdbcClient.sql("insert into point_accounts (user_id, balance) values ('user-1', 0)").update();
		insertOrderAndEvent("order-1", "event-1", NOW.minusSeconds(2));
		insertOrderAndEvent("order-2", "event-2", NOW.minusSeconds(1));
	}

	@AfterEach
	void tearDown() {
		clearData();
	}

	private void clearData() {
		jdbcClient.sql("delete from order_event_intents").update();
		jdbcClient.sql("delete from order_items").update();
		jdbcClient.sql("delete from orders").update();
		jdbcClient.sql("delete from point_accounts").update();
	}

	@Test
	void separate_claims_receive_different_events() {
		var first = repository.claim(1, NOW, Duration.ofSeconds(30));
		var second = repository.claim(1, NOW, Duration.ofSeconds(30));

		assertThat(first).extracting("eventId").containsExactly("event-1");
		assertThat(second).extracting("eventId").containsExactly("event-2");
	}

	@Test
	void expired_processing_lease_can_be_reclaimed() {
		var first = repository.claim(1, NOW, Duration.ofSeconds(30)).getFirst();

		var reclaimed = repository.claim(1, NOW.plusSeconds(31), Duration.ofSeconds(30));

		assertThat(reclaimed).extracting("eventId").contains(first.eventId());
		assertThat(reclaimed.getFirst().attempts()).isEqualTo(2);
	}

	@Test
	void published_event_is_not_claimed_again() {
		var event = repository.claim(1, NOW, Duration.ofSeconds(30)).getFirst();
		repository.markPublished(event.eventId(), NOW);

		assertThat(repository.claim(2, NOW.plusSeconds(31), Duration.ofSeconds(30)))
				.extracting("eventId").containsExactly("event-2");
	}

	private void insertOrderAndEvent(String orderId, String eventId, Instant occurredAt) {
		jdbcClient.sql("""
				insert into orders (order_id, user_id, status, total_payment_amount, remaining_point_balance, completed_at)
				values (:orderId, 'user-1', 'COMPLETED', 0, 0, :occurredAt)
				""").param("orderId", orderId).param("occurredAt", Timestamp.from(occurredAt)).update();
		jdbcClient.sql("""
				insert into order_event_intents
				       (event_id, order_id, event_name, payload, occurred_at, delivery_state, available_at)
				values (:eventId, :orderId, 'OrderCompleted', '{}'::jsonb, :occurredAt, 'PENDING', :occurredAt)
				""").param("eventId", eventId).param("orderId", orderId)
				.param("occurredAt", Timestamp.from(occurredAt)).update();
	}
}
