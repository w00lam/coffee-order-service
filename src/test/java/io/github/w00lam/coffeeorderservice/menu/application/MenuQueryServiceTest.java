package io.github.w00lam.coffeeorderservice.menu.application;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import io.github.w00lam.coffeeorderservice.menu.domain.MenuAvailability;
import java.util.List;
import org.junit.jupiter.api.Test;

class MenuQueryServiceTest {

	@Test
	void 저장소가_제공한_주문_가능_메뉴를_반환한다() {
		Menu menu = new Menu("menu-1", "아메리카노", 4_500, MenuAvailability.ORDERABLE);
		MenuRepository repository = () -> List.of(menu);

		List<Menu> result = new MenuQueryService(repository).getOrderableMenus();

		assertThat(result).containsExactly(menu);
	}

	@Test
	void 주문_가능_메뉴가_없으면_빈_목록을_반환한다() {
		MenuRepository repository = List::of;

		assertThat(new MenuQueryService(repository).getOrderableMenus()).isEmpty();
	}
}
