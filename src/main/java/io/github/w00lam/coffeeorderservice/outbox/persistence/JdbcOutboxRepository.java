package io.github.w00lam.coffeeorderservice.outbox.persistence;

import io.github.w00lam.coffeeorderservice.outbox.application.OutboxEvent;
import io.github.w00lam.coffeeorderservice.outbox.application.OutboxRepository;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JdbcOutboxRepository implements OutboxRepository {
	private final JdbcClient jdbcClient;

	public JdbcOutboxRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	@Transactional
	public List<OutboxEvent> claim(int batchSize, Instant now, Duration leaseDuration) {
		return jdbcClient.sql("""
				with candidates as (
				    select event_id
				      from order_event_intents
				     where (delivery_state = 'PENDING' and available_at <= :now)
				        or (delivery_state = 'PROCESSING' and lease_until <= :now)
				     order by available_at, occurred_at, event_id
				     for update skip locked
				     limit :batchSize
				)
				update order_event_intents event
				   set delivery_state = 'PROCESSING', lease_until = :leaseUntil,
				       attempts = attempts + 1, last_error = null
				  from candidates
				 where event.event_id = candidates.event_id
				returning event.event_id, event.order_id, event.event_name,
				          event.payload::text, event.occurred_at, event.attempts
				""").param("now", Timestamp.from(now)).param("leaseUntil", Timestamp.from(now.plus(leaseDuration)))
				.param("batchSize", batchSize)
				.query((rs, rowNum) -> new OutboxEvent(rs.getString("event_id"), rs.getString("order_id"),
						rs.getString("event_name"), rs.getString("payload"),
						rs.getTimestamp("occurred_at").toInstant(), rs.getInt("attempts")))
				.list();
	}

	@Override
	public void markPublished(String eventId, Instant publishedAt) {
		jdbcClient.sql("""
				update order_event_intents
				   set delivery_state = 'PUBLISHED', published_at = :publishedAt, lease_until = null
				 where event_id = :eventId and delivery_state = 'PROCESSING'
				""").param("publishedAt", Timestamp.from(publishedAt)).param("eventId", eventId).update();
	}

	@Override
	public void reschedule(String eventId, Instant availableAt, String error) {
		jdbcClient.sql("""
				update order_event_intents
				   set delivery_state = 'PENDING', available_at = :availableAt,
				       lease_until = null, last_error = :error
				 where event_id = :eventId and delivery_state = 'PROCESSING'
				""").param("availableAt", Timestamp.from(availableAt)).param("error", error)
				.param("eventId", eventId).update();
	}

	@Override
	public void markFailed(String eventId, String error) {
		jdbcClient.sql("""
				update order_event_intents
				   set delivery_state = 'FAILED', lease_until = null, last_error = :error
				 where event_id = :eventId and delivery_state = 'PROCESSING'
				""").param("error", error).param("eventId", eventId).update();
	}
}
