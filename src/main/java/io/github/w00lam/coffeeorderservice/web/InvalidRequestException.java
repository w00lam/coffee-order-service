package io.github.w00lam.coffeeorderservice.web;

public class InvalidRequestException extends RuntimeException {

	public InvalidRequestException(String message) {
		super(message);
	}
}
