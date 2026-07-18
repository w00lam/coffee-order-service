package io.github.w00lam.coffeeorderservice.outbox.observability;

import static io.github.w00lam.coffeeorderservice.observability.CoffeeMetricNames.KAFKA_PRODUCER_SEND_LATENCY;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class KafkaPublisherMetrics {
	private final Timer sendLatency;

	public KafkaPublisherMetrics(MeterRegistry meterRegistry) {
		this.sendLatency = meterRegistry.timer(KAFKA_PRODUCER_SEND_LATENCY);
	}

	public void record(InterruptiblePublish publish) throws InterruptedException, ExecutionException {
		long startedAt = System.nanoTime();
		try {
			publish.run();
		} finally {
			sendLatency.record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
		}
	}

	@FunctionalInterface
	public interface InterruptiblePublish {
		void run() throws InterruptedException, ExecutionException;
	}
}
