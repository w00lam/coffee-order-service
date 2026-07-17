package io.github.w00lam.coffeeorderservice.point.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeResult;
import io.github.w00lam.coffeeorderservice.point.application.PointChargeService;
import io.github.w00lam.coffeeorderservice.point.domain.PointAmount;
import io.github.w00lam.coffeeorderservice.web.InvalidRequestException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PointChargeController {

	private final AuthenticatedUserProvider authenticatedUserProvider;
	private final PointChargeService pointChargeService;

	public PointChargeController(
			AuthenticatedUserProvider authenticatedUserProvider,
			PointChargeService pointChargeService) {
		this.authenticatedUserProvider = authenticatedUserProvider;
		this.pointChargeService = pointChargeService;
	}

	@PostMapping("/point-charges")
	public PointChargeResponse charge(@RequestBody PointChargeRequest request) {
		PointAmount amount = request.toPointAmount();
		String userId = authenticatedUserProvider.requireUserId();
		return PointChargeResponse.from(pointChargeService.charge(userId, amount));
	}

	public static final class PointChargeRequest {
		private Object amount;
		private final Set<String> unexpectedFields = new HashSet<>();

		@JsonSetter("amount")
		public void setAmount(Object amount) {
			this.amount = amount;
		}

		@JsonAnySetter
		public void addUnexpectedField(String name, Object value) {
			unexpectedFields.add(name);
		}

		private PointAmount toPointAmount() {
			if (!unexpectedFields.isEmpty()) {
				throw new InvalidRequestException("Unexpected request fields: " + unexpectedFields);
			}
			if (amount == null) {
				throw new InvalidRequestException("amount is required");
			}
			if (!(amount instanceof Byte || amount instanceof Short || amount instanceof Integer
					|| amount instanceof Long || amount instanceof BigInteger)) {
				throw new InvalidChargeAmountException();
			}
			try {
				BigInteger integer = new BigInteger(amount.toString());
				return new PointAmount(integer.longValueExact());
			} catch (ArithmeticException | IllegalArgumentException exception) {
				throw new InvalidChargeAmountException();
			}
		}
	}

	public record PointChargeResponse(long chargedAmount, long balance, Instant chargedAt) {

		private static PointChargeResponse from(PointChargeResult result) {
			return new PointChargeResponse(result.chargedAmount(), result.balance(), result.chargedAt());
		}
	}
}
