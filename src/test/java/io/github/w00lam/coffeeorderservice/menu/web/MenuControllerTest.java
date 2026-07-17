package io.github.w00lam.coffeeorderservice.menu.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.w00lam.coffeeorderservice.menu.application.MenuQueryService;
import io.github.w00lam.coffeeorderservice.menu.domain.Menu;
import io.github.w00lam.coffeeorderservice.menu.domain.MenuAvailability;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MenuQueryService menuQueryService;

	@Test
	void 주문_가능_메뉴의_필드와_값을_200으로_반환한다() throws Exception {
		given(menuQueryService.getOrderableMenus()).willReturn(List.of(
				new Menu("menu-1", "아메리카노", 4_500, MenuAvailability.ORDERABLE)));

		mockMvc.perform(get("/menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.menus.length()").value(1))
				.andExpect(jsonPath("$.menus[0].menuId").value("menu-1"))
				.andExpect(jsonPath("$.menus[0].name").value("아메리카노"))
				.andExpect(jsonPath("$.menus[0].price").value(4_500))
				.andExpect(jsonPath("$.menus[0].availability").doesNotExist());
	}

	@Test
	void 메뉴가_없으면_빈_배열을_200으로_반환한다() throws Exception {
		given(menuQueryService.getOrderableMenus()).willReturn(List.of());

		mockMvc.perform(get("/menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.menus").isArray())
				.andExpect(jsonPath("$.menus").isEmpty());
	}

	@Test
	void 데이터베이스를_일시적으로_사용할_수_없으면_공통_503_오류를_반환한다() throws Exception {
		given(menuQueryService.getOrderableMenus())
				.willThrow(new DataAccessResourceFailureException("database unavailable"));

		mockMvc.perform(get("/menus"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").isNotEmpty())
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details").isEmpty())
				.andExpect(jsonPath("$.occurredAt").isNotEmpty());
	}

	@Test
	void 예상하지_못한_실패는_공통_500_오류를_반환한다() throws Exception {
		given(menuQueryService.getOrderableMenus()).willThrow(new IllegalStateException("unexpected"));

		mockMvc.perform(get("/menus"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.message").isNotEmpty())
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(jsonPath("$.details").isEmpty())
				.andExpect(jsonPath("$.occurredAt").isNotEmpty());
	}
}
