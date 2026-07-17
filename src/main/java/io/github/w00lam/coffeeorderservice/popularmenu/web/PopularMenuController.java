package io.github.w00lam.coffeeorderservice.popularmenu.web;

import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuItemResult;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuQueryService;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuResult;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PopularMenuController {
	private final PopularMenuQueryService queryService;
	private final Clock clock;

	public PopularMenuController(PopularMenuQueryService queryService, Clock clock) {
		this.queryService = queryService;
		this.clock = clock;
	}

	@GetMapping("/popular-menus")
	public PopularMenuResponse popularMenus() {
		return PopularMenuResponse.from(queryService.query(clock.instant()));
	}

	public record PopularMenuItemResponse(
			int rank, String menuId, String name, boolean orderable,
			BigInteger totalQuantity, BigInteger orderCount) {
		private static PopularMenuItemResponse from(PopularMenuItemResult item) {
			return new PopularMenuItemResponse(item.rank(), item.menuId(), item.name(), item.orderable(),
					item.totalQuantity(), item.orderCount());
		}
	}

	public record PopularMenuResponse(
			Instant asOf, Instant windowStart, String businessTimeZone, List<PopularMenuItemResponse> menus) {
		private static PopularMenuResponse from(PopularMenuResult result) {
			return new PopularMenuResponse(result.asOf(), result.windowStart(), "Asia/Seoul",
					result.menus().stream().map(PopularMenuItemResponse::from).toList());
		}
	}
}
