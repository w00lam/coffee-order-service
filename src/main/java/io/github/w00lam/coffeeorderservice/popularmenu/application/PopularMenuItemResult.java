package io.github.w00lam.coffeeorderservice.popularmenu.application;

import java.math.BigInteger;

public record PopularMenuItemResult(
		int rank, String menuId, String name, boolean orderable,
		BigInteger totalQuantity, BigInteger orderCount) {
}
