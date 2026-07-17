package io.github.w00lam.coffeeorderservice.popularmenu.kafka;

import java.time.Instant;
import java.util.List;

public record OrderCompletedMessage(
		int schemaVersion,
		String eventId,
		String eventType,
		String orderId,
		String userId,
		List<OrderCompletedItemMessage> items,
		long totalPaymentAmount,
		Instant occurredAt) {
	public record OrderCompletedItemMessage(String menuId, long quantity, long unitPrice, long lineAmount) {
	}
}
