package io.github.w00lam.coffeeorderservice.point.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PointAmountTest {

	@Test
	void 충전금액은_1_이상이어야_한다() {
		assertThatThrownBy(() -> new PointAmount(0))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("amount");
	}
}
