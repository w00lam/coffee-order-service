package io.github.w00lam.coffeeorderservice.datacollection.application;

import java.time.Instant;
import java.util.List;

public record DataCollectionOrder(
		String eventId,
		String orderId,
		String userId,
		List<Item> items,
		long totalPaymentAmount,
		Instant occurredAt) {

	public record Item(String menuId, long quantity) {
	}
}
