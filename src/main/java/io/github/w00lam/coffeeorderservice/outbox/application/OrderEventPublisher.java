package io.github.w00lam.coffeeorderservice.outbox.application;

public interface OrderEventPublisher {
	void publish(OutboxEvent event);
}
