package io.github.w00lam.coffeeorderservice.order.application;

import java.time.Instant;
import java.util.List;

public interface OrderRepository {
	boolean userExists(String userId);

	TokenClaim claimToken(String token, String userId, String fingerprint, Instant now);

	List<MenuSnapshot> findMenus(List<String> menuIds);

	Long deductPoints(String userId, long amount);

	void saveCompletedOrder(String orderId, String userId, List<OrderItemResult> items,
			long totalPaymentAmount, long remainingPointBalance, Instant completedAt, String eventId);

	void confirmSuccess(String token, String orderId, Instant confirmedAt);

	void confirmBusinessFailure(String token, String code, String message, Instant occurredAt);

	record MenuSnapshot(String menuId, long price, String availability) {
	}

	record TokenClaim(TokenClaimStatus status, OrderResult orderResult,
			String failureCode, String failureMessage, Instant failureOccurredAt) {
		public static TokenClaim acquired() {
			return new TokenClaim(TokenClaimStatus.ACQUIRED, null, null, null, null);
		}
	}

	enum TokenClaimStatus {
		ACQUIRED, SUCCEEDED, BUSINESS_FAILED, NOT_FOUND, EXPIRED, CONFLICT
	}
}
