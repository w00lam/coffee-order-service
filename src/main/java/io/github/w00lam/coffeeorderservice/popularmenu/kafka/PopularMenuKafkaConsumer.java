package io.github.w00lam.coffeeorderservice.popularmenu.kafka;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import io.github.w00lam.coffeeorderservice.popularmenu.application.OrderCompletedProjectionEvent;
import io.github.w00lam.coffeeorderservice.popularmenu.application.OrderCompletedProjectionItem;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuProjectionUpdater;
import java.util.HashSet;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.annotation.BackOff;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coffee.kafka.enabled", havingValue = "true")
public class PopularMenuKafkaConsumer {
	private final ObjectMapper objectMapper;
	private final PopularMenuProjectionUpdater updater;

	public PopularMenuKafkaConsumer(ObjectMapper objectMapper, PopularMenuProjectionUpdater updater) {
		this.objectMapper = objectMapper;
		this.updater = updater;
	}

	@KafkaListener(topics = "${coffee.kafka.order-events-topic}", groupId = "${coffee.kafka.popular-menu-group-id}")
	@RetryableTopic(
			attempts = "${coffee.kafka.popular-menu.retry-attempts}",
			backOff = @BackOff(
					delayString = "${coffee.kafka.popular-menu.retry-initial-backoff}",
					multiplierString = "${coffee.kafka.popular-menu.retry-multiplier}",
					maxDelayString = "${coffee.kafka.popular-menu.retry-maximum-backoff}"),
			retryTopicSuffix = "${coffee.kafka.popular-menu.retry-topic-suffix}",
			dltTopicSuffix = "${coffee.kafka.popular-menu.dlt-topic-suffix}",
			autoCreateTopics = "${coffee.kafka.popular-menu.auto-create-topics}",
			exclude = IllegalArgumentException.class)
	public void consume(ConsumerRecord<String, String> record) {
		OrderCompletedMessage message = deserialize(record.value());
		validate(record.key(), message);
		updater.apply(new OrderCompletedProjectionEvent(message.eventId(), message.orderId(), message.occurredAt(),
				message.items().stream()
						.map(item -> new OrderCompletedProjectionItem(item.menuId(), item.quantity()))
						.toList()));
	}

	private OrderCompletedMessage deserialize(String payload) {
		try {
			return objectMapper.readValue(payload, OrderCompletedMessage.class);
		} catch (JacksonException exception) {
			throw new IllegalArgumentException("Invalid OrderCompleted JSON", exception);
		}
	}

	private void validate(String key, OrderCompletedMessage message) {
		if (message.schemaVersion() != 1 || !"OrderCompleted".equals(message.eventType())) {
			throw new IllegalArgumentException("Unsupported order event schema");
		}
		if (isBlank(message.eventId()) || isBlank(message.orderId()) || isBlank(message.userId())
				|| message.occurredAt() == null || !message.orderId().equals(key)
				|| message.totalPaymentAmount() < 0 || message.items() == null || message.items().isEmpty()) {
			throw new IllegalArgumentException("Invalid OrderCompleted event");
		}
		var menuIds = new HashSet<String>();
		for (var item : message.items()) {
			if (isBlank(item.menuId()) || item.quantity() < 1 || item.unitPrice() < 0 || item.lineAmount() < 0
					|| !menuIds.add(item.menuId())) {
				throw new IllegalArgumentException("Invalid OrderCompleted item");
			}
		}
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
