package io.github.w00lam.coffeeorderservice.outbox.kafka;

import io.github.w00lam.coffeeorderservice.outbox.application.OrderEventPublisher;
import io.github.w00lam.coffeeorderservice.outbox.application.OutboxEvent;
import io.github.w00lam.coffeeorderservice.outbox.observability.KafkaPublisherMetrics;
import java.util.concurrent.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coffee.kafka.enabled", havingValue = "true")
public class KafkaOrderEventPublisher implements OrderEventPublisher {
	private final KafkaTemplate<String, String> kafkaTemplate;
	private final String topic;
	private final KafkaPublisherMetrics metrics;

	public KafkaOrderEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
			@Value("${coffee.kafka.order-events-topic}") String topic, KafkaPublisherMetrics metrics) {
		this.kafkaTemplate = kafkaTemplate;
		this.topic = topic;
		this.metrics = metrics;
	}

	@Override
	public void publish(OutboxEvent event) {
		try {
			metrics.record(() -> kafkaTemplate.send(topic, event.orderId(), event.payload()).get());
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Kafka publication was interrupted", exception);
		} catch (ExecutionException exception) {
			throw new IllegalStateException("Kafka publication failed", exception.getCause());
		}
	}
}
