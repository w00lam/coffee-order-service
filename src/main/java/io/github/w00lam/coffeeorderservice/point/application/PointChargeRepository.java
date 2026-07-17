package io.github.w00lam.coffeeorderservice.point.application;

@FunctionalInterface
public interface PointChargeRepository {

	PointChargeResult charge(String userId, long amount);
}
