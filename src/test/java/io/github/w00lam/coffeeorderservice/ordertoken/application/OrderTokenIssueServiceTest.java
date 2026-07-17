package io.github.w00lam.coffeeorderservice.ordertoken.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.ordertoken.domain.OrderToken;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class OrderTokenIssueServiceTest {

	@Test
	void 인증된_사용자에게_주문_토큰을_발급한다() {
		AtomicReference<OrderToken> saved = new AtomicReference<>();
		OrderTokenRepository repository = token -> saved.set(token);
		Clock clock = Clock.fixed(Instant.parse("2026-07-17T12:00:00Z"), ZoneOffset.UTC);

		OrderToken result = new OrderTokenIssueService(repository, () -> "token-1", clock).issue("user-1");

		assertThat(result.value()).isEqualTo("token-1");
		assertThat(result.userId()).isEqualTo("user-1");
		assertThat(result.expiresAt()).isEqualTo(Instant.parse("2026-07-17T12:10:00Z"));
		assertThat(saved.get()).isEqualTo(result);
	}
}
