package io.github.w00lam.coffeeorderservice.auth.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class HeaderAuthenticatedUserProviderTest {

	@AfterEach
	void clearRequest() {
		RequestContextHolder.resetRequestAttributes();
	}

	@Test
	void returns_user_from_configured_header() {
		var request = new MockHttpServletRequest();
		request.addHeader("X-User-Id", "user-1");
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

		assertThat(new HeaderAuthenticatedUserProvider("X-User-Id").requireUserId()).isEqualTo("user-1");
	}

	@Test
	void rejects_request_without_user_header() {
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

		assertThatThrownBy(() -> new HeaderAuthenticatedUserProvider("X-User-Id").requireUserId())
				.isInstanceOf(UnauthenticatedException.class);
	}
}
