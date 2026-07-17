package io.github.w00lam.coffeeorderservice.ordertoken.application;

import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;

@FunctionalInterface
public interface OrderTokenRepository {

	void save(OrderToken token);
}
