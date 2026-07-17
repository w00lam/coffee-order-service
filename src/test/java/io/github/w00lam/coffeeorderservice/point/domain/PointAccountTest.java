package io.github.w00lam.coffeeorderservice.point.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PointAccountTest {

	@Test
	void 잔액은_음수일_수_없다() {
		assertThatThrownBy(() -> new PointAccount("user-1", -1))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("balance");
	}
}
