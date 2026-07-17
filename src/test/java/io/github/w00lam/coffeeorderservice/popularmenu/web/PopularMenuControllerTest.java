package io.github.w00lam.coffeeorderservice.popularmenu.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuItemResult;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuQueryService;
import io.github.w00lam.coffeeorderservice.popularmenu.application.PopularMenuResult;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.math.BigInteger;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PopularMenuController.class)
class PopularMenuControllerTest {
	private static final Instant AS_OF = Instant.parse("2026-07-17T12:30:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PopularMenuQueryService queryService;

	@MockitoBean
	private Clock clock;

	@Test
	void returns_popular_menu_contract() throws Exception {
		given(clock.instant()).willReturn(AS_OF);
		given(queryService.query(AS_OF)).willReturn(new PopularMenuResult(AS_OF, AS_OF.minusSeconds(168 * 3600),
				List.of(new PopularMenuItemResult(1, "menu-1", "아메리카노", false,
						BigInteger.valueOf(120), BigInteger.valueOf(85)))));

		mockMvc.perform(get("/popular-menus"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.asOf").value("2026-07-17T12:30:00Z"))
				.andExpect(jsonPath("$.windowStart").value("2026-07-10T12:30:00Z"))
				.andExpect(jsonPath("$.businessTimeZone").value("Asia/Seoul"))
				.andExpect(jsonPath("$.menus[0].rank").value(1))
				.andExpect(jsonPath("$.menus[0].orderable").value(false))
				.andExpect(jsonPath("$.menus[0].totalQuantity").value(120))
				.andExpect(jsonPath("$.menus[0].orderCount").value(85));
	}
}
