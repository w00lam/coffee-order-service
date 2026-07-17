package io.github.w00lam.coffeeorderservice.point.domain;

public record PointAmount(long value) {

	public PointAmount {
		if (value < 1) {
			throw new IllegalArgumentException("point amount must be at least 1");
		}
	}
}
