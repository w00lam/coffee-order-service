package io.github.w00lam.coffeeorderservice.menu.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class MenuTest {

	@Test
	void 가격은_0원_이상이어야_한다() {
		assertThatThrownBy(() -> new Menu("menu-1", "아메리카노", -1, MenuAvailability.ORDERABLE))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("price");
	}

	@Test
	void 주문_가능_상태는_반드시_존재해야_한다() {
		assertThatThrownBy(() -> new Menu("menu-1", "아메리카노", 4_500, null))
				.isInstanceOf(NullPointerException.class)
				.hasMessageContaining("availability");
	}
}
