package io.github.w00lam.coffeeorderservice.order.application;

import java.time.Instant;

public class ConfirmedOrderBusinessException extends RuntimeException {
	private final String code;
	private final Instant occurredAt;

	public ConfirmedOrderBusinessException(String code, String message, Instant occurredAt) {
		super(message);
		this.code = code;
		this.occurredAt = occurredAt;
	}

	public String code() {
		return code;
	}

	public String getCode() {
		return code;
	}

	public Instant occurredAt() {
		return occurredAt;
	}
}
