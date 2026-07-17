package io.github.w00lam.coffeeorderservice.ordertoken.web;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.ordertoken.application.OrderTokenIssueService;
import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import io.github.w00lam.coffeeorderservice.web.InvalidRequestException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderTokenController {

	private final AuthenticatedUserProvider authenticatedUserProvider;
	private final OrderTokenIssueService orderTokenIssueService;

	public OrderTokenController(
			AuthenticatedUserProvider authenticatedUserProvider,
			OrderTokenIssueService orderTokenIssueService) {
		this.authenticatedUserProvider = authenticatedUserProvider;
		this.orderTokenIssueService = orderTokenIssueService;
	}

	@PostMapping("/order-tokens")
	@ResponseStatus(HttpStatus.CREATED)
	public OrderTokenResponse issue(@RequestBody(required = false) Object body) {
		if (body != null) {
			throw new InvalidRequestException("Request body is not allowed");
		}
		String userId = authenticatedUserProvider.requireUserId();
		return OrderTokenResponse.from(orderTokenIssueService.issue(userId));
	}

	public record OrderTokenResponse(String orderToken, Instant issuedAt, Instant expiresAt) {

		private static OrderTokenResponse from(OrderToken token) {
			return new OrderTokenResponse(token.value(), token.issuedAt(), token.expiresAt());
		}
	}
}
