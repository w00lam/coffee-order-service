package io.github.w00lam.coffeeorderservice.order.application;

public record OrderItemResult(String menuId, long quantity, long unitPrice, long lineAmount) {
}
