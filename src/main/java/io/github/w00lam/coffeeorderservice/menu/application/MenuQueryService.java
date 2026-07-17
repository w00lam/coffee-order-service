package io.github.w00lam.coffeeorderservice.menu.application;

import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class MenuQueryService {

	private final MenuRepository menuRepository;

	public MenuQueryService(MenuRepository menuRepository) {
		this.menuRepository = menuRepository;
	}

	public List<Menu> getOrderableMenus() {
		return menuRepository.findOrderable();
	}
}
