package io.github.w00lam.coffeeorderservice.outbox.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.KAFKA_PRODUCER_SEND_LATENCY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.Test;

class KafkaPublisherMetricsTest {
	@Test
	void records_both_successful_and_failed_send_attempts() throws Exception {
		var registry = new SimpleMeterRegistry();
		var metrics = new KafkaPublisherMetrics(registry);

		metrics.record(() -> { });
		assertThatThrownBy(() -> metrics.record(() -> {
			throw new ExecutionException(new IllegalStateException("broker unavailable"));
		})).isInstanceOf(ExecutionException.class);

		assertThat(registry.timer(KAFKA_PRODUCER_SEND_LATENCY).count()).isEqualTo(2);
	}
}
