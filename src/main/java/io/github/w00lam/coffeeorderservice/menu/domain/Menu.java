package io.github.w00lam.coffeeorderservice.menu.domain;

import java.util.Objects;

public record Menu(String id, String name, long price, MenuAvailability availability) {

	public Menu {
		if (id == null || id.isBlank()) {
			throw new IllegalArgumentException("menu id must not be blank");
		}
		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("menu name must not be blank");
		}
		if (price < 0) {
			throw new IllegalArgumentException("menu price must not be negative");
		}
		Objects.requireNonNull(availability, "menu availability must not be null");
	}
}
