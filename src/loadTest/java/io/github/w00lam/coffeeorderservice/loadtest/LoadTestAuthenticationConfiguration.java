package io.github.w00lam.coffeeorderservice.loadtest;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration(proxyBeanMethods = false)
class LoadTestAuthenticationConfiguration {
	static final String USER_HEADER = "X-Load-Test-User";

	@Bean
	@Primary
	AuthenticatedUserProvider loadTestAuthenticatedUserProvider() {
		return () -> {
			var attributes = RequestContextHolder.getRequestAttributes();
			if (!(attributes instanceof ServletRequestAttributes servletAttributes)) {
				throw new UnauthenticatedException();
			}
			String userId = servletAttributes.getRequest().getHeader(USER_HEADER);
			if (userId == null || userId.isBlank()) {
				throw new UnauthenticatedException();
			}
			return userId;
		};
	}
}
