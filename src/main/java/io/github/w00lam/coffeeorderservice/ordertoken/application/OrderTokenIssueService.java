package io.github.w00lam.coffeeorderservice.ordertoken.application;

import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;

@Service
public class OrderTokenIssueService {

	private final OrderTokenRepository orderTokenRepository;
	private final OrderTokenGenerator orderTokenGenerator;
	private final Clock clock;

	public OrderTokenIssueService(
			OrderTokenRepository orderTokenRepository,
			OrderTokenGenerator orderTokenGenerator,
			Clock clock) {
		this.orderTokenRepository = orderTokenRepository;
		this.orderTokenGenerator = orderTokenGenerator;
		this.clock = clock;
	}

	public OrderToken issue(String userId) {
		Instant issuedAt = clock.instant();
		OrderToken token = OrderToken.issue(orderTokenGenerator.generate(), userId, issuedAt);
		orderTokenRepository.save(token);
		return token;
	}
}
