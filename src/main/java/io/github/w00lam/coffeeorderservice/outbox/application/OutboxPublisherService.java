package io.github.w00lam.coffeeorderservice.outbox.application;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
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
	private final Counter claimed;
	private final Counter published;
	private final Counter failed;
	private final Timer publicationLatency;

	public OutboxPublisherService(OutboxRepository repository, OrderEventPublisher publisher, MeterRegistry meterRegistry) {
		this.repository = repository;
		this.publisher = publisher;
		this.claimed = meterRegistry.counter("coffee.outbox.claimed");
		this.published = meterRegistry.counter("coffee.outbox.published");
		this.failed = meterRegistry.counter("coffee.outbox.failed");
		this.publicationLatency = meterRegistry.timer("coffee.outbox.publication.latency");
	}

	public int publishBatch(int batchSize, Instant now, Duration leaseDuration,
			Duration initialBackoff, Duration maximumBackoff) {
		// claim 트랜잭션을 끝낸 뒤 Kafka를 호출해 DB 잠금을 네트워크 처리 동안 유지하지 않는다.
		// Kafka 전송 성공 후 상태 기록 전에 중단되면 재발행될 수 있으므로 Consumer가 eventId로 멱등 처리한다.
		var events = repository.claim(batchSize, now, leaseDuration);
		claimed.increment(events.size());
		for (OutboxEvent event : events) {
			try {
				publisher.publish(event);
				repository.markPublished(event.eventId(), now);
				published.increment();
				publicationLatency.record(Duration.between(event.occurredAt(), Instant.now()));
			} catch (RuntimeException exception) {
				failed.increment();
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
