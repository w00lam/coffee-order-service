package io.github.w00lam.coffeeorderservice.auth.application;

public class UnauthenticatedException extends RuntimeException {

	public UnauthenticatedException() {
		super("Authenticated user information is unavailable");
	}
}
