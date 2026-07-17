package io.github.w00lam.coffeeorderservice.outbox.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxPublisherServiceTest {
	private static final Instant NOW = Instant.parse("2026-07-17T12:00:00Z");

	@Mock
	private OutboxRepository repository;

	@Mock
	private OrderEventPublisher publisher;

	@Test
	void marks_event_published_after_kafka_acknowledgement() {
		OutboxEvent event = event(NOW.minusSeconds(10), 1);
		given(repository.claim(10, NOW, Duration.ofSeconds(30))).willReturn(List.of(event));
		var service = new OutboxPublisherService(repository, publisher);

		assertThat(service.publishBatch(10, NOW, Duration.ofSeconds(30),
				Duration.ofSeconds(1), Duration.ofMinutes(1))).isEqualTo(1);

		verify(publisher).publish(event);
		verify(repository).markPublished("event-1", NOW);
	}

	@Test
	void reschedules_temporary_failure_with_exponential_backoff() {
		OutboxEvent event = event(NOW.minusSeconds(10), 3);
		given(repository.claim(10, NOW, Duration.ofSeconds(30))).willReturn(List.of(event));
		doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(event);
		var service = new OutboxPublisherService(repository, publisher);

		service.publishBatch(10, NOW, Duration.ofSeconds(30), Duration.ofSeconds(1), Duration.ofMinutes(1));

		verify(repository).reschedule("event-1", NOW.plusSeconds(4),
				"IllegalStateException: broker unavailable");
	}

	@Test
	void classifies_failure_after_24_hours_as_final() {
		OutboxEvent event = event(NOW.minus(Duration.ofHours(24)), 4);
		given(repository.claim(10, NOW, Duration.ofSeconds(30))).willReturn(List.of(event));
		doThrow(new IllegalStateException("broker unavailable")).when(publisher).publish(event);
		var service = new OutboxPublisherService(repository, publisher);

		service.publishBatch(10, NOW, Duration.ofSeconds(30), Duration.ofSeconds(1), Duration.ofMinutes(1));

		verify(repository).markFailed("event-1", "IllegalStateException: broker unavailable");
	}

	private OutboxEvent event(Instant occurredAt, int attempts) {
		return new OutboxEvent("event-1", "order-1", "OrderCompleted", "{}", occurredAt, attempts);
	}
}
