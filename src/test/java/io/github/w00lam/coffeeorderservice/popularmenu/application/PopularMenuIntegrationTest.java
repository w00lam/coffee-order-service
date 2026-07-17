package io.github.w00lam.coffeeorderservice.popularmenu.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.time.Instant;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class PopularMenuIntegrationTest extends PostgreSqlIntegrationTest {
	private static final Instant AS_OF = Instant.parse("2026-07-17T12:30:00Z");

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private PopularMenuProjectionUpdater updater;

	@Autowired
	private PopularMenuQueryService queryService;

	@BeforeEach
	void setUp() {
		clearProjection();
		insertMenu("menu-a", "아메리카노", "NOT_ORDERABLE");
		insertMenu("menu-b", "카페라테", "ORDERABLE");
		insertMenu("menu-c", "에스프레소", "ORDERABLE");
		insertMenu("menu-d", "콜드브루", "ORDERABLE");
	}

	@AfterEach
	void tearDown() {
		clearProjection();
	}

	private void clearProjection() {
		jdbcClient.sql("delete from popular_menu_hourly_stats").update();
		jdbcClient.sql("delete from popular_menu_contributions").update();
		jdbcClient.sql("delete from popular_menu_processed_events").update();
		jdbcClient.sql("delete from menus").update();
	}

	@Test
	void aggregates_exact_168_hour_window_and_applies_ranking_rules() {
		apply("event-1", AS_OF.minusSeconds(167 * 3600), item("menu-a", 5), item("menu-b", 2));
		apply("event-2", AS_OF.minusSeconds(2 * 3600), item("menu-b", 3));
		apply("event-3", AS_OF.minusSeconds(60), item("menu-c", 5));
		apply("event-4", AS_OF.minusSeconds(30), item("menu-d", 1));
		apply("outside", AS_OF.minusSeconds(168 * 3600), item("menu-d", 100));

		PopularMenuResult result = queryService.query(AS_OF);

		assertThat(result.windowStart()).isEqualTo(AS_OF.minusSeconds(168 * 3600));
		assertThat(result.menus()).extracting(PopularMenuItemResult::menuId)
				.containsExactly("menu-b", "menu-a", "menu-c");
		assertThat(result.menus().get(0).totalQuantity()).isEqualTo(BigInteger.valueOf(5));
		assertThat(result.menus().get(0).orderCount()).isEqualTo(BigInteger.valueOf(2));
		assertThat(result.menus().get(1).orderable()).isFalse();
	}

	@Test
	void duplicate_event_is_applied_only_once() {
		var item = item("menu-a", 3);
		updater.apply(new OrderCompletedProjectionEvent("event-1", "order-1", AS_OF.minusSeconds(60), List.of(item)));
		updater.apply(new OrderCompletedProjectionEvent("event-1", "order-1", AS_OF.minusSeconds(60), List.of(item)));

		PopularMenuItemResult result = queryService.query(AS_OF).menus().getFirst();

		assertThat(result.totalQuantity()).isEqualTo(BigInteger.valueOf(3));
		assertThat(result.orderCount()).isEqualTo(BigInteger.ONE);
	}

	@Test
	void concurrent_duplicate_delivery_has_one_business_effect() throws Exception {
		var event = new OrderCompletedProjectionEvent("event-1", "order-1", AS_OF.minusSeconds(60),
				List.of(item("menu-a", 3)));
		var start = new CountDownLatch(1);
		try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
			var first = executor.submit(() -> { start.await(); return updater.apply(event); });
			var second = executor.submit(() -> { start.await(); return updater.apply(event); });
			start.countDown();
			assertThat(List.of(first.get(), second.get())).containsExactlyInAnyOrder(true, false);
		}

		assertThat(queryService.query(AS_OF).menus().getFirst().totalQuantity())
				.isEqualTo(BigInteger.valueOf(3));
	}

	@Test
	void returns_empty_list_when_window_has_no_sales() {
		assertThat(queryService.query(AS_OF).menus()).isEmpty();
	}

	private void apply(String eventId, Instant completedAt, OrderCompletedProjectionItem... items) {
		updater.apply(new OrderCompletedProjectionEvent(eventId, "order-" + eventId, completedAt, List.of(items)));
	}

	private OrderCompletedProjectionItem item(String menuId, long quantity) {
		return new OrderCompletedProjectionItem(menuId, quantity);
	}

	private void insertMenu(String id, String name, String availability) {
		jdbcClient.sql("insert into menus (menu_id, name, current_price, availability) values (:id, :name, 1000, :availability)")
				.param("id", id).param("name", name).param("availability", availability).update();
	}
}
