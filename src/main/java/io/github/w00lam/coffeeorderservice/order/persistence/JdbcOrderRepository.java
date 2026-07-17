package io.github.w00lam.coffeeorderservice.order.persistence;

import io.github.w00lam.coffeeorderservice.order.application.OrderItemResult;
import io.github.w00lam.coffeeorderservice.order.application.OrderRepository;
import io.github.w00lam.coffeeorderservice.order.application.OrderResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcOrderRepository implements OrderRepository {
	private final JdbcClient jdbcClient;

	public JdbcOrderRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public boolean userExists(String userId) {
		return jdbcClient.sql("select exists(select 1 from point_accounts where user_id = :userId)")
				.param("userId", userId).query(Boolean.class).single();
	}

	@Override
	public TokenClaim claimToken(String token, String userId, String fingerprint, Instant now) {
		int updated = jdbcClient.sql("""
				update order_tokens
				   set request_fingerprint = :fingerprint
				 where order_token = :token
				   and user_id = :userId
				   and status = 'AVAILABLE'
				   and expires_at > :now
				   and request_fingerprint is null
				""").param("fingerprint", fingerprint).param("token", token)
				.param("userId", userId).param("now", Timestamp.from(now)).update();
		if (updated == 1) {
			return TokenClaim.acquired();
		}

		TokenRow row = jdbcClient.sql("""
				select user_id, status, expires_at, request_fingerprint, order_id,
				       confirmed_body ->> 'code' as failure_code,
				       confirmed_body ->> 'message' as failure_message,
				       confirmed_at
				  from order_tokens
				 where order_token = :token
				""").param("token", token)
				.query((rs, rowNum) -> new TokenRow(rs.getString("user_id"), rs.getString("status"),
						rs.getTimestamp("expires_at").toInstant(), rs.getString("request_fingerprint"),
						rs.getString("order_id"), rs.getString("failure_code"), rs.getString("failure_message"),
						rs.getTimestamp("confirmed_at") == null ? null : rs.getTimestamp("confirmed_at").toInstant()))
				.optional().orElse(null);
		if (row == null) {
			return new TokenClaim(TokenClaimStatus.NOT_FOUND, null, null, null, null);
		}
		if (!row.userId().equals(userId)) {
			return new TokenClaim(TokenClaimStatus.CONFLICT, null, null, null, null);
		}
		if ("AVAILABLE".equals(row.status()) && !row.expiresAt().isAfter(now)) {
			return new TokenClaim(TokenClaimStatus.EXPIRED, null, null, null, null);
		}
		if (!fingerprint.equals(row.fingerprint())) {
			return new TokenClaim(TokenClaimStatus.CONFLICT, null, null, null, null);
		}
		if ("SUCCEEDED".equals(row.status())) {
			return new TokenClaim(TokenClaimStatus.SUCCEEDED, findOrder(row.orderId()), null, null, null);
		}
		if ("BUSINESS_FAILED".equals(row.status())) {
			return new TokenClaim(TokenClaimStatus.BUSINESS_FAILED, null,
					row.failureCode(), row.failureMessage(), row.confirmedAt());
		}
		return new TokenClaim(TokenClaimStatus.CONFLICT, null, null, null, null);
	}

	@Override
	public List<MenuSnapshot> findMenus(List<String> menuIds) {
		return jdbcClient.sql("""
				select menu_id, current_price, availability
				  from menus
				 where menu_id in (:menuIds)
				""").param("menuIds", menuIds)
				.query((rs, rowNum) -> new MenuSnapshot(rs.getString("menu_id"),
						rs.getLong("current_price"), rs.getString("availability"))).list();
	}

	@Override
	public Long deductPoints(String userId, long amount) {
		return jdbcClient.sql("""
				update point_accounts
				   set balance = balance - :amount
				 where user_id = :userId and balance >= :amount
				 returning balance
				""").param("amount", amount).param("userId", userId)
				.query(Long.class).optional().orElse(null);
	}

	@Override
	public void saveCompletedOrder(String orderId, String userId, List<OrderItemResult> items,
			long totalPaymentAmount, long remainingPointBalance, Instant completedAt, String eventId) {
		jdbcClient.sql("""
				insert into orders (order_id, user_id, status, total_payment_amount,
				                    remaining_point_balance, completed_at)
				values (:orderId, :userId, 'COMPLETED', :total, :remaining, :completedAt)
				""").param("orderId", orderId).param("userId", userId).param("total", totalPaymentAmount)
				.param("remaining", remainingPointBalance).param("completedAt", Timestamp.from(completedAt)).update();
		for (OrderItemResult item : items) {
			jdbcClient.sql("""
					insert into order_items (order_id, menu_id, quantity, unit_price_snapshot, line_amount)
					values (:orderId, :menuId, :quantity, :unitPrice, :lineAmount)
					""").param("orderId", orderId).param("menuId", item.menuId())
					.param("quantity", item.quantity()).param("unitPrice", item.unitPrice())
					.param("lineAmount", item.lineAmount()).update();
		}
		jdbcClient.sql("""
				insert into order_event_intents (event_id, order_id, event_name, payload, occurred_at, delivery_state)
				select :eventId, :orderId, 'OrderCompleted',
				       jsonb_build_object(
				           'schemaVersion', 1, 'eventId', :eventId,
				           'eventType', 'OrderCompleted', 'orderId', :orderId,
				           'userId', :userId, 'items',
				           (select jsonb_agg(jsonb_build_object(
				               'menuId', menu_id, 'quantity', quantity,
				               'unitPrice', unit_price_snapshot, 'lineAmount', line_amount)
				            order by menu_id) from order_items where order_id = :orderId),
				           'totalPaymentAmount', :total, 'occurredAt', cast(:completedAt as text)),
				       :completedAt, 'PENDING'
				""").param("eventId", eventId).param("orderId", orderId).param("userId", userId)
				.param("total", totalPaymentAmount).param("completedAt", Timestamp.from(completedAt)).update();
	}

	@Override
	public void confirmSuccess(String token, String orderId, Instant confirmedAt) {
		jdbcClient.sql("""
				update order_tokens
				   set status = 'SUCCEEDED', confirmed_http_status = 200,
				       confirmed_at = :confirmedAt, order_id = :orderId
				 where order_token = :token and status = 'AVAILABLE'
				""").param("confirmedAt", Timestamp.from(confirmedAt)).param("orderId", orderId)
				.param("token", token).update();
	}

	@Override
	public void confirmBusinessFailure(String token, String code, String message, Instant occurredAt) {
		jdbcClient.sql("""
				update order_tokens
				   set status = 'BUSINESS_FAILED', confirmed_http_status = 422,
				       confirmed_body = jsonb_build_object(
				           'code', :code, 'message', :message, 'details', jsonb_build_array(),
				           'occurredAt', cast(:occurredAt as text)),
				       confirmed_at = :occurredAt
				 where order_token = :token and status = 'AVAILABLE'
				""").param("code", code).param("message", message)
				.param("occurredAt", Timestamp.from(occurredAt)).param("token", token).update();
	}

	private OrderResult findOrder(String orderId) {
		OrderRow order = jdbcClient.sql("""
				select total_payment_amount, remaining_point_balance, completed_at
				  from orders where order_id = :orderId
				""").param("orderId", orderId)
				.query((rs, rowNum) -> new OrderRow(rs.getLong("total_payment_amount"),
						rs.getLong("remaining_point_balance"), rs.getTimestamp("completed_at").toInstant())).single();
		List<OrderItemResult> items = jdbcClient.sql("""
				select menu_id, quantity, unit_price_snapshot, line_amount
				  from order_items where order_id = :orderId order by menu_id
				""").param("orderId", orderId)
				.query((rs, rowNum) -> new OrderItemResult(rs.getString("menu_id"), rs.getLong("quantity"),
						rs.getLong("unit_price_snapshot"), rs.getLong("line_amount"))).list();
		return new OrderResult(orderId, items, order.total(), order.remaining(), order.completedAt());
	}

	private record TokenRow(String userId, String status, Instant expiresAt, String fingerprint,
			String orderId, String failureCode, String failureMessage, Instant confirmedAt) {
	}

	private record OrderRow(long total, long remaining, Instant completedAt) {
	}
}
