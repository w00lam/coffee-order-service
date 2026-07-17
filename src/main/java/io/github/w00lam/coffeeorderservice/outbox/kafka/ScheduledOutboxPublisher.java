package io.github.w00lam.coffeeorderservice.outbox.kafka;

import io.github.w00lam.coffeeorderservice.outbox.application.OutboxPublisherService;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "coffee.kafka.enabled", havingValue = "true")
public class ScheduledOutboxPublisher {
	private final OutboxPublisherService service;
	private final Clock clock;
	private final int batchSize;
	private final Duration leaseDuration;
	private final Duration initialBackoff;
	private final Duration maximumBackoff;

	public ScheduledOutboxPublisher(OutboxPublisherService service, Clock clock,
			@Value("${coffee.kafka.publisher.batch-size}") int batchSize,
			@Value("${coffee.kafka.publisher.lease-duration}") Duration leaseDuration,
			@Value("${coffee.kafka.publisher.retry-initial-backoff}") Duration initialBackoff,
			@Value("${coffee.kafka.publisher.retry-maximum-backoff}") Duration maximumBackoff) {
		this.service = service;
		this.clock = clock;
		this.batchSize = batchSize;
		this.leaseDuration = leaseDuration;
		this.initialBackoff = initialBackoff;
		this.maximumBackoff = maximumBackoff;
	}

	@Scheduled(fixedDelayString = "${coffee.kafka.publisher.poll-interval}")
	public void publishPendingEvents() {
		service.publishBatch(batchSize, clock.instant(), leaseDuration, initialBackoff, maximumBackoff);
	}
}
