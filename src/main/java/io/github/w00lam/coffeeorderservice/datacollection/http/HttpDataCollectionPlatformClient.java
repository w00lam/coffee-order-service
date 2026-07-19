package io.github.w00lam.coffeeorderservice.datacollection.http;

import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionOrder;
import io.github.w00lam.coffeeorderservice.datacollection.application.DataCollectionPlatformClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "coffee.data-collection.enabled", havingValue = "true")
public class HttpDataCollectionPlatformClient implements DataCollectionPlatformClient {
	private final RestClient restClient;
	private final String ordersPath;

	public HttpDataCollectionPlatformClient(RestClient.Builder builder,
			@Value("${coffee.data-collection.base-url}") String baseUrl,
			@Value("${coffee.data-collection.orders-path:/orders}") String ordersPath) {
		this.restClient = builder.baseUrl(baseUrl).build();
		this.ordersPath = ordersPath;
	}

	@Override
	public void send(DataCollectionOrder order) {
		restClient.post().uri(ordersPath).header("Idempotency-Key", order.eventId())
				.body(order).retrieve().toBodilessEntity();
	}
}
