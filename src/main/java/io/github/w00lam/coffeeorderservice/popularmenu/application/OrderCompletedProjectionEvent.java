package io.github.w00lam.coffeeorderservice.popularmenu.application;

import java.time.Instant;
import java.util.List;

public record OrderCompletedProjectionEvent(
		String eventId, String orderId, Instant completedAt, List<OrderCompletedProjectionItem> items) {
	public OrderCompletedProjectionEvent {
		items = List.copyOf(items);
	}
}
