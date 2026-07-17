package io.github.w00lam.coffeeorderservice.menu.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import io.github.w00lam.coffeeorderservice.support.PostgreSqlIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@Import(JdbcMenuRepository.class)
@ActiveProfiles("test")
class JdbcMenuRepositoryTest extends PostgreSqlIntegrationTest {

	@Autowired
	private JdbcClient jdbcClient;

	@Autowired
	private JdbcMenuRepository repository;

	@BeforeEach
	void clearMenus() {
		jdbcClient.sql("delete from menus").update();
	}

	@Test
	void 주문_가능_메뉴만_조회한다() {
		insertMenu("menu-1", "아메리카노", 4_500, "ORDERABLE");
		insertMenu("menu-2", "카페라테", 5_000, "NOT_ORDERABLE");

		assertThat(repository.findOrderable())
				.extracting(Menu::id, Menu::name, Menu::price)
				.containsExactly(tuple("menu-1", "아메리카노", 4_500L));
	}

	@Test
	void 저장된_메뉴가_없으면_빈_목록을_조회한다() {
		assertThat(repository.findOrderable()).isEmpty();
	}

	@Test
	void 데이터베이스도_음수_가격을_거부한다() {
		assertThatThrownBy(() -> insertMenu("menu-1", "아메리카노", -1, "ORDERABLE"))
				.isInstanceOf(Exception.class);
	}

	@Test
	void 데이터베이스도_정의되지_않은_상태를_거부한다() {
		assertThatThrownBy(() -> insertMenu("menu-1", "아메리카노", 4_500, "UNKNOWN"))
				.isInstanceOf(Exception.class);
	}

	private void insertMenu(String id, String name, long price, String availability) {
		jdbcClient.sql("""
				insert into menus (menu_id, name, current_price, availability)
				values (:id, :name, :price, :availability)
				""")
				.param("id", id)
				.param("name", name)
				.param("price", price)
				.param("availability", availability)
				.update();
	}
}
