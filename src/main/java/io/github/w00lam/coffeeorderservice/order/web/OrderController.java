package io.github.w00lam.coffeeorderservice.order.web;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.github.w00lam.coffeeorderservice.auth.application.AuthenticatedUserProvider;
import io.github.w00lam.coffeeorderservice.order.application.OrderApplicationService;
import io.github.w00lam.coffeeorderservice.order.application.OrderItemResult;
import io.github.w00lam.coffeeorderservice.order.application.OrderLineCommand;
import io.github.w00lam.coffeeorderservice.order.application.OrderResult;
import io.github.w00lam.coffeeorderservice.web.InvalidRequestException;
import java.math.BigInteger;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {
	private final AuthenticatedUserProvider authenticatedUserProvider;
	private final OrderApplicationService orderApplicationService;

	public OrderController(AuthenticatedUserProvider authenticatedUserProvider,
			OrderApplicationService orderApplicationService) {
		this.authenticatedUserProvider = authenticatedUserProvider;
		this.orderApplicationService = orderApplicationService;
	}

	@PostMapping("/orders")
	public OrderResponse placeOrder(
			@RequestHeader(name = "Order-Token", required = false) String token,
			@RequestBody OrderRequest request) {
		List<OrderLineCommand> lines = request.toCommands();
		if (token == null || token.isBlank()) {
			throw new InvalidRequestException("Order-Token header is required");
		}
		String userId = authenticatedUserProvider.requireUserId();
		return OrderResponse.from(orderApplicationService.placeOrder(userId, token, lines));
	}

	public static final class OrderRequest {
		private List<OrderItemRequest> items;
		private final Set<String> unexpectedFields = new HashSet<>();

		@JsonSetter("items")
		public void setItems(List<OrderItemRequest> items) {
			this.items = items;
		}

		@JsonAnySetter
		public void addUnexpectedField(String name, Object value) {
			unexpectedFields.add(name);
		}

		private List<OrderLineCommand> toCommands() {
			if (!unexpectedFields.isEmpty()) {
				throw new InvalidRequestException("Unexpected request fields: " + unexpectedFields);
			}
			if (items == null || items.isEmpty()) {
				throw new OrderRequestValidationException("EMPTY_ORDER_ITEMS", "주문 항목이 필요합니다.");
			}
			List<OrderLineCommand> commands = items.stream().map(OrderItemRequest::toCommand).toList();
			Set<String> menuIds = new HashSet<>();
			if (commands.stream().anyMatch(command -> !menuIds.add(command.menuId()))) {
				throw new OrderRequestValidationException("DUPLICATE_MENU_ID", "같은 메뉴를 중복 요청할 수 없습니다.");
			}
			return commands;
		}
	}

	public static final class OrderItemRequest {
		private Object menuId;
		private Object quantity;
		private final Set<String> unexpectedFields = new HashSet<>();

		@JsonSetter("menuId")
		public void setMenuId(Object menuId) {
			this.menuId = menuId;
		}

		@JsonSetter("quantity")
		public void setQuantity(Object quantity) {
			this.quantity = quantity;
		}

		@JsonAnySetter
		public void addUnexpectedField(String name, Object value) {
			unexpectedFields.add(name);
		}

		private OrderLineCommand toCommand() {
			if (!unexpectedFields.isEmpty()) {
				throw new InvalidRequestException("Unexpected order item fields: " + unexpectedFields);
			}
			if (!(menuId instanceof String value) || value.isBlank()) {
				throw new InvalidRequestException("menuId is required");
			}
			if (!(quantity instanceof Byte || quantity instanceof Short || quantity instanceof Integer
					|| quantity instanceof Long || quantity instanceof BigInteger)) {
				throw invalidQuantity();
			}
			try {
				long valueQuantity = new BigInteger(quantity.toString()).longValueExact();
				if (valueQuantity < 1) {
					throw invalidQuantity();
				}
				return new OrderLineCommand(value, valueQuantity);
			} catch (ArithmeticException | NumberFormatException exception) {
				throw invalidQuantity();
			}
		}

		private OrderRequestValidationException invalidQuantity() {
			return new OrderRequestValidationException("INVALID_QUANTITY", "수량은 1 이상의 정수여야 합니다.");
		}
	}

	public record OrderItemResponse(String menuId, long quantity, long unitPrice, long lineAmount) {
		private static OrderItemResponse from(OrderItemResult item) {
			return new OrderItemResponse(item.menuId(), item.quantity(), item.unitPrice(), item.lineAmount());
		}
	}

	public record OrderResponse(String orderId, String status, List<OrderItemResponse> items,
			long totalPaymentAmount, long remainingPointBalance, Instant completedAt) {
		private static OrderResponse from(OrderResult result) {
			return new OrderResponse(result.orderId(), "COMPLETED",
					result.items().stream().map(OrderItemResponse::from).toList(),
					result.totalPaymentAmount(), result.remainingPointBalance(), result.completedAt());
		}
	}
}
