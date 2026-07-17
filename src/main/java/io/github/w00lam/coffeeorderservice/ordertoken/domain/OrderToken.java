package io.github.w00lam.coffeeorderservice.ordertoken.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

public record OrderToken(
		String value,
		String userId,
		OrderTokenStatus status,
		Instant issuedAt,
		Instant expiresAt) {

	private static final Duration UNUSED_VALIDITY = Duration.ofMinutes(10);

	public OrderToken {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("order token must not be blank");
		}
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("user id must not be blank");
		}
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(issuedAt, "issuedAt must not be null");
		Objects.requireNonNull(expiresAt, "expiresAt must not be null");
		if (!expiresAt.isAfter(issuedAt)) {
			throw new IllegalArgumentException("expiresAt must be after issuedAt");
		}
	}

	public static OrderToken issue(String value, String userId, Instant issuedAt) {
		return new OrderToken(value, userId, OrderTokenStatus.AVAILABLE, issuedAt, issuedAt.plus(UNUSED_VALIDITY));
	}
}
