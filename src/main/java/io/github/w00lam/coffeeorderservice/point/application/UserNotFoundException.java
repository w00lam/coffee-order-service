package io.github.w00lam.coffeeorderservice.point.application;

public class UserNotFoundException extends RuntimeException {

	public UserNotFoundException(String userId) {
		super("User not found: " + userId);
	}
}
