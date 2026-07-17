package io.github.w00lam.coffeeorderservice.outbox.application;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Service
@ConditionalOnProperty(name = "coffee.kafka.enabled", havingValue = "true")
public class OutboxPublisherService {
	private static final Duration MAX_RETRY_AGE = Duration.ofHours(24);
	private final OutboxRepository repository;
	private final OrderEventPublisher publisher;

	public OutboxPublisherService(OutboxRepository repository, OrderEventPublisher publisher) {
		this.repository = repository;
		this.publisher = publisher;
	}

	public int publishBatch(int batchSize, Instant now, Duration leaseDuration,
			Duration initialBackoff, Duration maximumBackoff) {
		var events = repository.claim(batchSize, now, leaseDuration);
		for (OutboxEvent event : events) {
			try {
				publisher.publish(event);
				repository.markPublished(event.eventId(), now);
			} catch (RuntimeException exception) {
				String error = conciseError(exception);
				if (!event.occurredAt().plus(MAX_RETRY_AGE).isAfter(now)) {
					repository.markFailed(event.eventId(), error);
				} else {
					repository.reschedule(event.eventId(), now.plus(backoff(event.attempts() - 1, initialBackoff, maximumBackoff)), error);
				}
			}
		}
		return events.size();
	}

	private Duration backoff(int previousAttempts, Duration initial, Duration maximum) {
		long multiplier = 1L << Math.min(previousAttempts, 30);
		try {
			Duration candidate = initial.multipliedBy(multiplier);
			return candidate.compareTo(maximum) > 0 ? maximum : candidate;
		} catch (ArithmeticException exception) {
			return maximum;
		}
	}

	private String conciseError(RuntimeException exception) {
		String message = exception.getMessage();
		return exception.getClass().getSimpleName() + (message == null ? "" : ": " + message);
	}
}
