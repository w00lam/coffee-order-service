package io.github.w00lam.coffeeorderservice.outbox.application;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

public interface OutboxRepository {
	List<OutboxEvent> claim(int batchSize, Instant now, Duration leaseDuration);

	void markPublished(String eventId, Instant publishedAt);

	void reschedule(String eventId, Instant availableAt, String error);

	void markFailed(String eventId, String error);
}
