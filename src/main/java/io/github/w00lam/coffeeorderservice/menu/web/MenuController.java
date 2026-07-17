package io.github.w00lam.coffeeorderservice.menu.web;

import io.github.w00lam.coffeeorderservice.menu.application.MenuQueryService;
import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MenuController {

	private final MenuQueryService menuQueryService;

	public MenuController(MenuQueryService menuQueryService) {
		this.menuQueryService = menuQueryService;
	}

	@GetMapping("/menus")
	public MenuListResponse getMenus() {
		List<MenuResponse> menus = menuQueryService.getOrderableMenus().stream()
				.map(MenuResponse::from)
				.toList();
		return new MenuListResponse(menus);
	}

	public record MenuListResponse(List<MenuResponse> menus) {
	}

	public record MenuResponse(String menuId, String name, long price) {

		private static MenuResponse from(Menu menu) {
			return new MenuResponse(menu.id(), menu.name(), menu.price());
		}
	}
}
