package io.github.w00lam.coffeeorderservice.ordertoken.application;

@FunctionalInterface
public interface OrderTokenGenerator {

	String generate();
}
