package io.github.w00lam.coffeeorderservice.ordertoken.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.ordertoken.application.OrderTokenIssueService;
import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderTokenController.class)
class OrderTokenControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticatedUserProvider authenticatedUserProvider;

	@MockitoBean
	private OrderTokenIssueService orderTokenIssueService;

	@Test
	void 주문_토큰을_201로_발급한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("user-1");
		given(orderTokenIssueService.issue("user-1")).willReturn(OrderToken.issue(
				"token-1", "user-1", Instant.parse("2026-07-17T12:00:00Z")));

		mockMvc.perform(post("/order-tokens"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.orderToken").value("token-1"))
				.andExpect(jsonPath("$.issuedAt").value("2026-07-17T12:00:00Z"))
				.andExpect(jsonPath("$.expiresAt").value("2026-07-17T12:10:00Z"));
	}

	@Test
	void Body를_보내면_거부한다() throws Exception {
		mockMvc.perform(post("/order-tokens")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userId\":\"user-1\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willThrow(new UnauthenticatedException());

		mockMvc.perform(post("/order-tokens"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 사용자가_없으면_404를_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("missing-user");
		given(orderTokenIssueService.issue("missing-user"))
				.willThrow(new UserNotFoundException("missing-user"));

		mockMvc.perform(post("/order-tokens"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}
}
