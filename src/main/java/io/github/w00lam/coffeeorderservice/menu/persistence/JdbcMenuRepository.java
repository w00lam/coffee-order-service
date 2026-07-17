package io.github.w00lam.coffeeorderservice.menu.persistence;

import io.github.w00lam.coffeeorderservice.menu.application.MenuRepository;
import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import io.github.w00lam.coffeeorderservice.menu.domain.MenuAvailability;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class JdbcMenuRepository implements MenuRepository {

	private final JdbcClient jdbcClient;

	public JdbcMenuRepository(JdbcClient jdbcClient) {
		this.jdbcClient = jdbcClient;
	}

	@Override
	public List<Menu> findOrderable() {
		return jdbcClient.sql("""
				select menu_id, name, current_price, availability
				from menus
				where availability = 'ORDERABLE'
				""")
				.query((resultSet, rowNumber) -> new Menu(
						resultSet.getString("menu_id"),
						resultSet.getString("name"),
						resultSet.getLong("current_price"),
						MenuAvailability.valueOf(resultSet.getString("availability"))))
				.list();
	}
}
