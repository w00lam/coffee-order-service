package io.github.w00lam.coffeeorderservice.ordertoken.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrderTokenTest {

	@Test
	void 사용하지_않은_토큰은_발급_후_10분간_유효하다() {
		Instant issuedAt = Instant.parse("2026-07-17T12:00:00Z");

		OrderToken token = OrderToken.issue("token-1", "user-1", issuedAt);

		assertThat(Duration.between(token.issuedAt(), token.expiresAt())).isEqualTo(Duration.ofMinutes(10));
		assertThat(token.status()).isEqualTo(OrderTokenStatus.AVAILABLE);
	}
}
