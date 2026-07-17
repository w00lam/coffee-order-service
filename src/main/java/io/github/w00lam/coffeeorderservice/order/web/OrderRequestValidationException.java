package io.github.w00lam.coffeeorderservice.order.web;

public class OrderRequestValidationException extends RuntimeException {
	private final String code;

	public OrderRequestValidationException(String code, String message) {
		super(message);
		this.code = code;
	}

	public String code() {
		return code;
	}
}
