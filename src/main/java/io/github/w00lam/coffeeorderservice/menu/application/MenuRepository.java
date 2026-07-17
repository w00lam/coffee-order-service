package io.github.w00lam.coffeeorderservice.menu.application;

import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import java.util.List;

@FunctionalInterface
public interface MenuRepository {

	List<Menu> findOrderable();
}
