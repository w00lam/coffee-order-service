package io.github.w00lam.coffeeorderservice.outbox.application;

import java.time.Instant;

public record OutboxEvent(
		String eventId, String orderId, String eventType, String payload, Instant occurredAt, int attempts) {
}
