package io.github.w00lam.coffeeorderservice.outbox.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_CLAIMED;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_FAILED;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_PUBLICATION_LATENCY;
import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.OUTBOX_PUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class OutboxMetricsTest {
	private static final Instant NOW = Instant.parse("2026-07-18T00:00:10Z");

	@Test
	void records_existing_outbox_metric_names_and_meanings() {
		var registry = new SimpleMeterRegistry();
		var metrics = new OutboxMetrics(registry, Clock.fixed(NOW, ZoneOffset.UTC));

		metrics.claimed(3);
		metrics.published(NOW.minusSeconds(10));
		metrics.failed();

		assertThat(registry.counter(OUTBOX_CLAIMED).count()).isEqualTo(3);
		assertThat(registry.counter(OUTBOX_PUBLISHED).count()).isEqualTo(1);
		assertThat(registry.counter(OUTBOX_FAILED).count()).isEqualTo(1);
		assertThat(registry.timer(OUTBOX_PUBLICATION_LATENCY).count()).isEqualTo(1);
		assertThat(registry.timer(OUTBOX_PUBLICATION_LATENCY).totalTime(TimeUnit.SECONDS)).isEqualTo(10);
	}
}
