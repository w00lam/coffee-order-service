package io.github.w00lam.coffeeorderservice.order.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.order.application.OrderApplicationService;
import io.github.w00lam.coffeeorderservice.order.application.OrderItemResult;
import io.github.w00lam.coffeeorderservice.order.application.OrderLineCommand;
import io.github.w00lam.coffeeorderservice.order.application.OrderResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticatedUserProvider authenticatedUserProvider;

	@MockitoBean
	private OrderApplicationService orderApplicationService;

	@Test
	void 주문_완료_응답을_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("user-1");
		given(orderApplicationService.placeOrder("user-1", "token-1", List.of(new OrderLineCommand("menu-1", 2))))
				.willReturn(new OrderResult("order-1", List.of(new OrderItemResult("menu-1", 2, 4_500, 9_000)),
						9_000, 11_000, Instant.parse("2026-07-17T12:00:00Z")));

		mockMvc.perform(post("/orders")
						.header("Order-Token", "token-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"menuId\":\"menu-1\",\"quantity\":2}]}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.orderId").value("order-1"))
				.andExpect(jsonPath("$.status").value("COMPLETED"))
				.andExpect(jsonPath("$.items[0].unitPrice").value(4_500))
				.andExpect(jsonPath("$.totalPaymentAmount").value(9_000))
				.andExpect(jsonPath("$.remainingPointBalance").value(11_000));
	}

	@Test
	void 빈_주문_항목은_거부한다() throws Exception {
		mockMvc.perform(post("/orders").header("Order-Token", "token-1")
						.contentType(MediaType.APPLICATION_JSON).content("{\"items\":[]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("EMPTY_ORDER_ITEMS"));
	}

	@Test
	void 중복_메뉴는_거부한다() throws Exception {
		mockMvc.perform(post("/orders").header("Order-Token", "token-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"menuId\":\"menu-1\",\"quantity\":1},{\"menuId\":\"menu-1\",\"quantity\":2}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DUPLICATE_MENU_ID"));
	}

	@Test
	void 유효하지_않은_수량은_거부한다() throws Exception {
		mockMvc.perform(post("/orders").header("Order-Token", "token-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"menuId\":\"menu-1\",\"quantity\":0}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_QUANTITY"));
	}

	@Test
	void 금지된_가격_필드는_거부한다() throws Exception {
		mockMvc.perform(post("/orders").header("Order-Token", "token-1")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"items\":[{\"menuId\":\"menu-1\",\"quantity\":1,\"unitPrice\":1}]}"))
				.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}
}
