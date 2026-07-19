package io.github.w00lam.coffeeorderservice.auth.web;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Primary
@ConditionalOnProperty(name = "coffee.auth.header.enabled", havingValue = "true")
public class HeaderAuthenticatedUserProvider implements AuthenticatedUserProvider {
	private final String headerName;

	public HeaderAuthenticatedUserProvider(@Value("${coffee.auth.header.name:X-User-Id}") String headerName) {
		this.headerName = headerName;
	}

	@Override
	public String requireUserId() {
		var attributes = RequestContextHolder.getRequestAttributes();
		if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
			throw new UnauthenticatedException();
		}
		String userId = servletAttributes.getRequest().getHeader(headerName);
		if (userId == null || userId.isBlank()) {
			throw new UnauthenticatedException();
		}
		return userId;
	}
}
