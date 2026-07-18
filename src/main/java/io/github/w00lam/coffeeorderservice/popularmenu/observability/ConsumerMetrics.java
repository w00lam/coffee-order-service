package io.github.w00lam.coffeeorderservice.popularmenu.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_DUPLICATES;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_EVENT_LATENCY;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_PROCESSED;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ConsumerMetrics {
	private final Counter processed;
	private final Counter duplicates;
	private final Timer eventLatency;
	private final Clock clock;

	public ConsumerMetrics(MeterRegistry meterRegistry, Clock clock) {
		this.processed = meterRegistry.counter(CONSUMER_PROCESSED);
		this.duplicates = meterRegistry.counter(CONSUMER_DUPLICATES);
		this.eventLatency = meterRegistry.timer(CONSUMER_EVENT_LATENCY);
		this.clock = clock;
	}

	public void processed(Instant occurredAt) {
		processed.increment();
		eventLatency.record(Duration.between(occurredAt, clock.instant()));
	}

	public void duplicate() {
		duplicates.increment();
	}
}
