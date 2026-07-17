package io.github.w00lam.coffeeorderservice.point.domain;

public record PointAccount(String userId, long balance) {

	public PointAccount {
		if (userId == null || userId.isBlank()) {
			throw new IllegalArgumentException("user id must not be blank");
		}
		if (balance < 0) {
			throw new IllegalArgumentException("point balance must not be negative");
		}
	}
}
