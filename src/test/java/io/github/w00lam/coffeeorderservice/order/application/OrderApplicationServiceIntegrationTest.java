package io.github.w00lam.coffeeorderservice.order.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class OrderApplicationServiceIntegrationTest extends PostgreSqlIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private OrderApplicationService service;

	@BeforeEach
	void setUp() {
		clearOrderData();
		jdbcClient.sql("insert into point_accounts (user_id, balance) values ('user-1', 20000)").update();
		insertMenu("menu-1", "아메리카노", 4_500, "ORDERABLE");
		insertToken("token-1", "user-1");
	}

	@AfterEach
	void tearDown() {
		clearOrderData();
	}

	private void clearOrderData() {
		jdbcClient.sql("delete from order_event_intents").update();
		jdbcClient.sql("delete from order_items").update();
		jdbcClient.sql("delete from orders").update();
		jdbcClient.sql("delete from order_tokens").update();
		jdbcClient.sql("delete from menus").update();
		jdbcClient.sql("delete from point_accounts").update();
	}

	@Test
	void 서버_가격으로_결제하고_주문_항목_Outbox_토큰_결과를_원자적으로_확정한다() {
		OrderResult result = service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 2)));

		assertThat(result.totalPaymentAmount()).isEqualTo(9_000);
		assertThat(result.remainingPointBalance()).isEqualTo(11_000);
		assertThat(result.items()).singleElement()
				.satisfies(item -> {
					assertThat(item.unitPrice()).isEqualTo(4_500);
					assertThat(item.lineAmount()).isEqualTo(9_000);
				});
		assertThat(count("orders")).isEqualTo(1);
		assertThat(count("order_items")).isEqualTo(1);
		assertThat(count("order_event_intents")).isEqualTo(1);
		assertThat(value("select status from order_tokens where order_token = 'token-1'", String.class))
				.isEqualTo("SUCCEEDED");
	}

	@Test
	void 같은_토큰과_같은_내용의_재시도는_최초_결과를_반환하고_중복_효과를_만들지_않는다() {
		OrderResult first = service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1)));

		OrderResult retry = service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1)));

		assertThat(retry).isEqualTo(first);
		assertThat(count("orders")).isEqualTo(1);
		assertThat(value("select balance from point_accounts where user_id = 'user-1'", Long.class)).isEqualTo(15_500);
	}

	@Test
	void 잔액이_부족하면_실패_결과만_확정하고_다시_시도해도_같은_오류를_반환한다() {
		jdbcClient.sql("update point_accounts set balance = 100 where user_id = 'user-1'").update();

		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1))))
				.isInstanceOf(ConfirmedOrderBusinessException.class)
				.extracting("code")
				.isEqualTo("INSUFFICIENT_POINTS");
		assertThat(count("orders")).isZero();
		assertThat(count("order_event_intents")).isZero();
		assertThat(value("select balance from point_accounts where user_id = 'user-1'", Long.class)).isEqualTo(100);
		assertThat(value("select status from order_tokens where order_token = 'token-1'", String.class))
				.isEqualTo("BUSINESS_FAILED");

		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1))))
				.isInstanceOf(ConfirmedOrderBusinessException.class)
				.extracting("code")
				.isEqualTo("INSUFFICIENT_POINTS");
	}

	@Test
	void 같은_토큰에_다른_주문_내용은_충돌한다() {
		service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1)));

		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 2))))
				.isInstanceOf(OrderTokenRequestConflictException.class);
	}

	@Test
	void 주문_불가_메뉴는_업무_실패로_확정한다() {
		jdbcClient.sql("update menus set availability = 'NOT_ORDERABLE' where menu_id = 'menu-1'").update();

		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1))))
				.isInstanceOf(ConfirmedOrderBusinessException.class)
				.extracting("code")
				.isEqualTo("MENU_NOT_ORDERABLE");
		assertThat(count("orders")).isZero();
	}

	@Test
	void missing_menu_is_confirmed_as_a_business_failure() {
		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("missing", 1))))
				.isInstanceOf(ConfirmedOrderBusinessException.class)
				.extracting("code")
				.isEqualTo("MENU_NOT_FOUND");
		assertThat(value("select status from order_tokens where order_token = 'token-1'", String.class))
				.isEqualTo("BUSINESS_FAILED");
	}

	@Test
	void unknown_token_is_not_found() {
		assertThatThrownBy(() -> service.placeOrder("user-1", "missing", List.of(new OrderLineCommand("menu-1", 1))))
				.isInstanceOf(OrderTokenNotFoundException.class);
	}

	@Test
	void unused_expired_token_is_gone() {
		jdbcClient.sql("""
				update order_tokens
				   set issued_at = clock_timestamp() - interval '2 minutes',
				       expires_at = clock_timestamp() - interval '1 minute'
				 where order_token = 'token-1'
				""")
				.update();

		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 1))))
				.isInstanceOf(OrderTokenExpiredException.class);
	}

	@Test
	void item_multiplication_overflow_is_confirmed() {
		assertThatThrownBy(() -> service.placeOrder("user-1", "token-1",
				List.of(new OrderLineCommand("menu-1", Long.MAX_VALUE))))
				.isInstanceOf(ConfirmedOrderBusinessException.class)
				.extracting("code")
				.isEqualTo("ORDER_AMOUNT_OUT_OF_RANGE");
	}

	private void insertMenu(String id, String name, long price, String availability) {
		jdbcClient.sql("insert into menus (menu_id, name, current_price, availability) values (:id, :name, :price, :availability)")
				.param("id", id).param("name", name).param("price", price).param("availability", availability).update();
	}

	private void insertToken(String token, String userId) {
		OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
		jdbcClient.sql("insert into order_tokens (order_token, user_id, status, issued_at, expires_at) values (:token, :userId, 'AVAILABLE', :issuedAt, :expiresAt)")
				.param("token", token).param("userId", userId).param("issuedAt", now).param("expiresAt", now.plusMinutes(10)).update();
	}

	private long count(String table) {
		return jdbcClient.sql("select count(*) from " + table).query(Long.class).single();
	}

	private <T> T value(String sql, Class<T> type) {
		return jdbcClient.sql(sql).query(type).single();
	}
}
