package io.github.w00lam.coffeeorderservice.observability;

public final class CoffeeMetricNames {
	public static final String OUTBOX_CLAIMED = "coffee.outbox.claimed";
	public static final String OUTBOX_PUBLISHED = "coffee.outbox.published";
	public static final String OUTBOX_FAILED = "coffee.outbox.failed";
	public static final String OUTBOX_PUBLICATION_LATENCY = "coffee.outbox.publication.latency";
	public static final String KAFKA_PRODUCER_SEND_LATENCY = "coffee.kafka.producer.send.latency";
	public static final String CONSUMER_PROCESSED = "coffee.consumer.processed";
	public static final String CONSUMER_DUPLICATES = "coffee.consumer.duplicates";
	public static final String CONSUMER_EVENT_LATENCY = "coffee.consumer.event.latency";

	private CoffeeMetricNames() {
	}
}
