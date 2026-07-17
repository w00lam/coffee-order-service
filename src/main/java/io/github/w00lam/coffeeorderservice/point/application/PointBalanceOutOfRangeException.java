package io.github.w00lam.coffeeorderservice.point.application;

public class PointBalanceOutOfRangeException extends RuntimeException {

	public PointBalanceOutOfRangeException() {
		super("Point balance exceeds the signed 64-bit range");
	}
}
