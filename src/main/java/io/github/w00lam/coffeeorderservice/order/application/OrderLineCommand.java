package io.github.w00lam.coffeeorderservice.order.application;

public record OrderLineCommand(String menuId, long quantity) {
}
