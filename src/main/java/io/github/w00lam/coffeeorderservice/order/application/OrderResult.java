package io.github.w00lam.coffeeorderservice.order.application;

import java.time.Instant;
import java.util.List;

public record OrderResult(
		String orderId,
		List<OrderItemResult> items,
		long totalPaymentAmount,
		long remainingPointBalance,
		Instant completedAt) {

	public OrderResult {
		items = List.copyOf(items);
	}
}
