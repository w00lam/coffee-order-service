package io.github.w00lam.coffeeorderservice.popularmenu.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_DUPLICATES;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_EVENT_LATENCY;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.CONSUMER_PROCESSED;
import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ConsumerMetricsTest {
	private static final Instant NOW = Instant.parse("2026-07-18T00:00:05Z");

	@Test
	void records_processed_duplicate_and_event_latency_metrics() {
		var registry = new SimpleMeterRegistry();
		var metrics = new ConsumerMetrics(registry, Clock.fixed(NOW, ZoneOffset.UTC));

		metrics.processed(NOW.minusSeconds(5));
		metrics.duplicate();

		assertThat(registry.counter(CONSUMER_PROCESSED).count()).isEqualTo(1);
		assertThat(registry.counter(CONSUMER_DUPLICATES).count()).isEqualTo(1);
		assertThat(registry.timer(CONSUMER_EVENT_LATENCY).count()).isEqualTo(1);
		assertThat(registry.timer(CONSUMER_EVENT_LATENCY).totalTime(TimeUnit.SECONDS)).isEqualTo(5);
	}
}
