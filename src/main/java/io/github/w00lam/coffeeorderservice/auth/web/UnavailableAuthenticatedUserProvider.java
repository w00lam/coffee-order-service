package io.github.w00lam.coffeeorderservice.auth.web;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import org.springframework.stereotype.Component;

@Component
public class UnavailableAuthenticatedUserProvider implements AuthenticatedUserProvider {

	@Override
	public String requireUserId() {
		throw new UnauthenticatedException();
	}
}
