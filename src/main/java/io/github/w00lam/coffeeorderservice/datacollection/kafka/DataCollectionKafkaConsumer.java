package io.github.w00lam.coffeeorderservice.datacollection.kafka;

import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionOrder;
import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionPlatformClient;
import io.github.w00lam.coffeeorderservice.order.event.OrderCompletedMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = {"coffee.kafka.enabled", "coffee.data-collection.enabled"}, havingValue = "true")
public class DataCollectionKafkaConsumer {
	private final ObjectMapper objectMapper;
	private final DataCollectionPlatformClient client;

	public DataCollectionKafkaConsumer(ObjectMapper objectMapper, DataCollectionPlatformClient client) {
		this.objectMapper = objectMapper;
		this.client = client;
	}

	@KafkaListener(topics = "${coffee.kafka.order-events-topic}", groupId = "${coffee.data-collection.group-id}")
	@RetryableTopic(attempts = "${coffee.data-collection.retry-attempts}",
			backOff = @BackOff(delayString = "${coffee.data-collection.retry-initial-backoff}",
					multiplierString = "${coffee.data-collection.retry-multiplier}",
					maxDelayString = "${coffee.data-collection.retry-maximum-backoff}"),
			retryTopicSuffix = "${coffee.data-collection.retry-topic-suffix}",
			dltTopicSuffix = "${coffee.data-collection.dlt-topic-suffix}",
			autoCreateTopics = "${coffee.data-collection.auto-create-topics}", exclude = IllegalArgumentException.class)
	public void consume(ConsumerRecord<String, String> record) {
		OrderCompletedMessage message = deserialize(record.value());
		validate(record.key(), message);
		client.send(new DataCollectionOrder(message.eventId(), message.orderId(), message.userId(),
				message.items().stream().map(item -> new DataCollectionOrder.Item(item.menuId(), item.quantity())).toList(),
				message.totalPaymentAmount(), message.occurredAt()));
	}

	private OrderCompletedMessage deserialize(String payload) {
		try {
			return objectMapper.readValue(payload, OrderCompletedMessage.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Invalid OrderCompleted JSON", exception);
		}
	}

	private void validate(String key, OrderCompletedMessage message) {
		if (message.schemaVersion() != 1 || !"OrderCompleted".equals(message.eventType())
				|| blank(message.eventId()) || blank(message.orderId()) || blank(message.userId())
				|| message.occurredAt() == null || !message.orderId().equals(key)
				|| message.totalPaymentAmount() < 0 || message.items() == null || message.items().isEmpty()
				|| message.items().stream().anyMatch(item -> blank(item.menuId()) || item.quantity() < 1)) {
			throw new IllegalArgumentException("Invalid OrderCompleted event");
		}
	}

	private boolean blank(String value) {
		return value == null || value.isBlank();
	}
}
