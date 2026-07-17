package io.github.w00lam.coffeeorderservice.popularmenu.application;

import java.time.Instant;
import java.util.List;

public record PopularMenuResult(Instant asOf, Instant windowStart, List<PopularMenuItemResult> menus) {
	public PopularMenuResult {
		menus = List.copyOf(menus);
	}
}
