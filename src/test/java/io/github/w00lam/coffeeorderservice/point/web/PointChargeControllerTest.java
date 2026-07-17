package io.github.w00lam.coffeeorderservice.point.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.point.application.PointBalanceOutOfRangeException;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeResult;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeService;
import io.github.w00lam.coffeeorderservice.point.domain.PointAmount;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PointChargeController.class)
class PointChargeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AuthenticatedUserProvider authenticatedUserProvider;

	@MockitoBean
	private PointChargeService pointChargeService;

	@Test
	void 인증된_사용자에게_포인트를_충전하고_결과를_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("user-1");
		given(pointChargeService.charge("user-1", new PointAmount(10_000)))
				.willReturn(new PointChargeResult(10_000, 25_000, Instant.parse("2026-07-17T12:00:00Z")));

		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.chargedAmount").value(10_000))
				.andExpect(jsonPath("$.balance").value(25_000))
				.andExpect(jsonPath("$.chargedAt").value("2026-07-17T12:00:00Z"));
	}

	@Test
	void 사용자_ID를_Body로_받지_않는다() throws Exception {
		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"userId\":\"user-1\",\"amount\":10000}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void 금액이_1_미만이면_거부한다() throws Exception {
		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":0}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHARGE_AMOUNT"));
	}

	@Test
	void 정수가_아닌_금액은_거부한다() throws Exception {
		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":1.5}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHARGE_AMOUNT"));
	}

	@Test
	void signed_64_bit_범위를_넘는_금액은_거부한다() throws Exception {
		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":9223372036854775808}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CHARGE_AMOUNT"));
	}

	@Test
	void 인증_정보가_없으면_401을_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willThrow(new UnauthenticatedException());

		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	@Test
	void 사용자가_없으면_404를_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("missing-user");
		given(pointChargeService.charge("missing-user", new PointAmount(10_000)))
				.willThrow(new UserNotFoundException("missing-user"));

		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	void 충전_후_잔액이_범위를_넘으면_422를_반환한다() throws Exception {
		given(authenticatedUserProvider.requireUserId()).willReturn("user-1");
		given(pointChargeService.charge("user-1", new PointAmount(10_000)))
				.willThrow(new PointBalanceOutOfRangeException());

		mockMvc.perform(post("/point-charges")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"amount\":10000}"))
				.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code").value("POINT_BALANCE_OUT_OF_RANGE"));
	}
}
