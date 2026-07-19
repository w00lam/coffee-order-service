package io.github.w00lam.coffeeorderservice.datacollection.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionOrder;
import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionPlatformClient;
import io.github.w00lam.coffeeorderservice.order.event.OrderCompletedMessage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class DataCollectionKafkaConsumerTest {

	@Test
	void sends_authenticated_user_items_and_payment_amount_to_platform() throws Exception {
		ObjectMapper objectMapper = mock(ObjectMapper.class);
		Instant occurredAt = Instant.parse("2026-07-19T01:00:00Z");
		var message = new OrderCompletedMessage(1, "event-1", "OrderCompleted", "order-1", "user-1",
				List.of(new OrderCompletedMessage.OrderCompletedItemMessage("menu-1", 2, 4500, 9000)),
				9000, occurredAt);
		when(objectMapper.readValue("payload", OrderCompletedMessage.class)).thenReturn(message);
		var sent = new ArrayList<DataCollectionOrder>();
		DataCollectionPlatformClient client = sent::add;
		var consumer = new DataCollectionKafkaConsumer(objectMapper, client);

		consumer.consume(new ConsumerRecord<>("orders", 0, 0, "order-1", "payload"));

		assertThat(sent).containsExactly(new DataCollectionOrder(
				"event-1", "order-1", "user-1",
				List.of(new DataCollectionOrder.Item("menu-1", 2)), 9000, occurredAt));
	}
}
