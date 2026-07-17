package io.github.w00lam.coffeeorderservice.point.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.point.domain.PointAmount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PointChargeServiceTest {

	@Test
	void 인증된_사용자의_포인트를_충전한다() {
		Instant chargedAt = Instant.parse("2026-07-17T12:00:00Z");
		PointChargeRepository repository = (userId, amount) -> new PointChargeResult(amount, 25_000, chargedAt);

		PointChargeResult result = new PointChargeService(repository)
				.charge("user-1", new PointAmount(10_000));

		assertThat(result.chargedAmount()).isEqualTo(10_000);
		assertThat(result.balance()).isEqualTo(25_000);
		assertThat(result.chargedAt()).isEqualTo(chargedAt);
	}
}
