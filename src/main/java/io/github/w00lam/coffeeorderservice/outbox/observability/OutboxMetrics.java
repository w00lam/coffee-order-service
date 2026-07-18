package io.github.w00lam.coffeeorderservice.outbox.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_CLAIMED;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_FAILED;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_PUBLICATION_LATENCY;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_PUBLISHED;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class OutboxMetrics {
	private final Counter claimed;
	private final Counter published;
	private final Counter failed;
	private final Timer publicationLatency;
	private final Clock clock;

	public OutboxMetrics(MeterRegistry meterRegistry, Clock clock) {
		this.claimed = meterRegistry.counter(OUTBOX_CLAIMED);
		this.published = meterRegistry.counter(OUTBOX_PUBLISHED);
		this.failed = meterRegistry.counter(OUTBOX_FAILED);
		this.publicationLatency = meterRegistry.timer(OUTBOX_PUBLICATION_LATENCY);
		this.clock = clock;
	}

	public void claimed(int count) {
		claimed.increment(count);
	}

	public void published(Instant occurredAt) {
		published.increment();
		publicationLatency.record(Duration.between(occurredAt, clock.instant()));
	}

	public void failed() {
		failed.increment();
	}
}
