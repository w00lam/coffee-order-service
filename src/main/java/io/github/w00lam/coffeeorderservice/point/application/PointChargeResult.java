package io.github.w00lam.coffeeorderservice.point.application;

import java.time.Instant;
import java.util.Objects;

public record PointChargeResult(long chargedAmount, long balance, Instant chargedAt) {

	public PointChargeResult {
		if (chargedAmount < 1) {
			throw new IllegalArgumentException("charged amount must be at least 1");
		}
		if (balance < 0) {
			throw new IllegalArgumentException("point balance must not be negative");
		}
		Objects.requireNonNull(chargedAt, "chargedAt must not be null");
	}
}
