package io.github.w00lam.coffeeorderservice.popularmenu.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.order.application.OrderApplicationService;
import io.github.w00lam.coffeeorderservice.order.application.OrderLineCommand;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuQueryService;
import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.time.Duration;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.kafka.KafkaContainer;

@SpringBootTest(properties = {
		"coffee.kafka.enabled=true",
		"coffee.kafka.order-events-topic=coffee-order-completed-test",
		"coffee.kafka.popular-menu-group-id=popular-menu-test",
		"coffee.kafka.publisher.poll-interval=100ms",
		"coffee.kafka.publisher.batch-size=10",
		"coffee.kafka.publisher.lease-duration=5s",
		"coffee.kafka.publisher.retry-initial-backoff=100ms",
		"coffee.kafka.publisher.retry-maximum-backoff=1s",
		"coffee.kafka.popular-menu.retry-attempts=3",
		"coffee.kafka.popular-menu.retry-initial-backoff=100ms",
		"coffee.kafka.popular-menu.retry-multiplier=2",
		"coffee.kafka.popular-menu.retry-maximum-backoff=1s",
		"coffee.kafka.popular-menu.retry-topic-suffix=-popular-retry",
		"coffee.kafka.popular-menu.dlt-topic-suffix=-popular-dlt",
		"coffee.kafka.popular-menu.auto-create-topics=true"
})
@ActiveProfiles("test")
class KafkaPopularMenuEndToEndTest extends PostgreSqlIntegrationTest {
	private static final KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0");

	static {
		kafka.start();
	}

	@DynamicPropertySource
	static void kafkaProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
	}

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private OrderApplicationService orderService;

	@Autowired
	private PopularMenuQueryService popularMenuQueryService;

	@Autowired
	private KafkaTemplate<String, String> kafkaTemplate;

	@BeforeEach
	void setUp() {
		clearData();
		jdbcClient.sql("insert into point_accounts (user_id, balance) values ('user-1', 20000)").update();
		jdbcClient.sql("insert into menus (menu_id, name, current_price, availability) values ('menu-1', '아메리카노', 4500, 'ORDERABLE')").update();
		Instant now = Instant.now();
		jdbcClient.sql("""
				insert into order_tokens (order_token, user_id, status, issued_at, expires_at)
				values ('token-1', 'user-1', 'AVAILABLE', :issuedAt, :expiresAt)
				""").param("issuedAt", Timestamp.from(now)).param("expiresAt", Timestamp.from(now.plusSeconds(600))).update();
	}

	@AfterEach
	void tearDown() {
		clearData();
	}

	@Test
	void completed_order_is_published_and_automatically_reflected_in_popular_menu_projection() throws Exception {
		orderService.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 2)));

		awaitProjection();

		var result = popularMenuQueryService.query(Instant.now()).menus().getFirst();
		assertThat(result.menuId()).isEqualTo("menu-1");
		assertThat(result.totalQuantity()).hasToString("2");
		assertThat(jdbcClient.sql("select delivery_state from order_event_intents")
				.query(String.class).single()).isEqualTo("PUBLISHED");
		assertThat(jdbcClient.sql("select count(*) from popular_menu_processed_events")
				.query(Long.class).single()).isEqualTo(1);
	}

	@Test
	void invalid_schema_is_isolated_in_popular_menu_dlt_without_projection_effect() throws Exception {
		kafkaTemplate.send("coffee-order-completed-test", "order-invalid", "{}").get();

		Properties properties = new Properties();
		properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
		properties.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-verifier-" + UUID.randomUUID());
		properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
		boolean found = false;
		try (var consumer = new KafkaConsumer<String, String>(properties)) {
			consumer.subscribe(List.of("coffee-order-completed-test-popular-dlt"));
			Instant deadline = Instant.now().plusSeconds(10);
			while (!found && Instant.now().isBefore(deadline)) {
				for (var record : consumer.poll(Duration.ofMillis(250)).records(
						"coffee-order-completed-test-popular-dlt")) {
					found = found || "{}".equals(record.value());
				}
			}
		}

		assertThat(found).isTrue();
		assertThat(jdbcClient.sql("select count(*) from popular_menu_processed_events")
				.query(Long.class).single()).isZero();
	}

	private void awaitProjection() throws Exception {
		Instant deadline = Instant.now().plusSeconds(15);
		while (Instant.now().isBefore(deadline)) {
			long count = jdbcClient.sql("select count(*) from popular_menu_processed_events")
					.query(Long.class).single();
			if (count == 1) {
				return;
			}
			Thread.sleep(100);
		}
		throw new AssertionError("OrderCompleted was not projected within 15 seconds");
	}

	private void clearData() {
		jdbcClient.sql("delete from popular_menu_hourly_stats").update();
		jdbcClient.sql("delete from popular_menu_contributions").update();
		jdbcClient.sql("delete from popular_menu_processed_events").update();
		jdbcClient.sql("delete from order_event_intents").update();
		jdbcClient.sql("delete from order_items").update();
		jdbcClient.sql("delete from orders").update();
		jdbcClient.sql("delete from order_tokens").update();
		jdbcClient.sql("delete from menus").update();
		jdbcClient.sql("delete from point_accounts").update();
	}
}
