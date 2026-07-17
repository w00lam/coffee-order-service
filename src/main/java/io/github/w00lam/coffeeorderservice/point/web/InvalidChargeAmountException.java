package io.github.w00lam.coffeeorderservice.point.web;

public class InvalidChargeAmountException extends RuntimeException {

	public InvalidChargeAmountException() {
		super("Charge amount must be an integer in the signed 64-bit range and at least 1");
	}
}
