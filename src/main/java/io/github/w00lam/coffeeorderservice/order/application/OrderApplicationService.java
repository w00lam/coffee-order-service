package io.github.w00lam.coffeeorderservice.order.application;

import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.order.application.OrderRepository.MenuSnapshot;
import io.github.w00lam.coffeeorderservice.order.application.OrderRepository.TokenClaim;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderApplicationService {
	private final OrderRepository repository;
	private final Clock clock;

	public OrderApplicationService(OrderRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	// 확정된 업무 실패는 토큰에 같은 응답을 재현할 수 있도록 커밋하고,
	// 예기치 않은 장애는 토큰 선점과 주문 변경을 모두 롤백한다.
	@Transactional(noRollbackFor = ConfirmedOrderBusinessException.class)
	public OrderResult placeOrder(String userId, String token, List<OrderLineCommand> lines) {
		if (!repository.userExists(userId)) {
			throw new UserNotFoundException(userId);
		}

		Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
		TokenClaim claim = repository.claimToken(token, userId, fingerprint(lines), now);
		switch (claim.status()) {
			case SUCCEEDED -> { return claim.orderResult(); }
			case BUSINESS_FAILED -> throw new ConfirmedOrderBusinessException(
					claim.failureCode(), claim.failureMessage(), claim.failureOccurredAt());
			case NOT_FOUND -> throw new OrderTokenNotFoundException();
			case EXPIRED -> throw new OrderTokenExpiredException();
			case CONFLICT -> throw new OrderTokenRequestConflictException();
			case ACQUIRED -> { }
		}

		Map<String, MenuSnapshot> menus = repository.findMenus(
				lines.stream().map(OrderLineCommand::menuId).toList()).stream()
				.collect(Collectors.toMap(MenuSnapshot::menuId, Function.identity()));
		if (menus.size() != lines.size()) {
			throw confirmed(token, "MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다.", now);
		}
		if (menus.values().stream().anyMatch(menu -> !"ORDERABLE".equals(menu.availability()))) {
			throw confirmed(token, "MENU_NOT_ORDERABLE", "주문할 수 없는 메뉴가 포함되어 있습니다.", now);
		}

		List<OrderItemResult> items;
		long total;
		try {
			items = lines.stream().map(line -> {
				MenuSnapshot menu = menus.get(line.menuId());
				return new OrderItemResult(line.menuId(), line.quantity(), menu.price(),
						Math.multiplyExact(menu.price(), line.quantity()));
			}).sorted(Comparator.comparing(OrderItemResult::menuId)).toList();
			total = items.stream().mapToLong(OrderItemResult::lineAmount)
					.reduce(0L, Math::addExact);
		} catch (ArithmeticException exception) {
			throw confirmed(token, "ORDER_AMOUNT_OUT_OF_RANGE", "주문금액이 허용 범위를 초과합니다.", now);
		}

		Long remainingBalance = repository.deductPoints(userId, total);
		if (remainingBalance == null) {
			throw confirmed(token, "INSUFFICIENT_POINTS", "포인트 잔액이 부족합니다.", now);
		}

		String orderId = UUID.randomUUID().toString();
		OrderResult result = new OrderResult(orderId, items, total, remainingBalance, now);
		repository.saveCompletedOrder(orderId, userId, items, total, remainingBalance, now,
				UUID.randomUUID().toString());
		repository.confirmSuccess(token, orderId, now);
		return result;
	}

	private ConfirmedOrderBusinessException confirmed(String token, String code, String message, Instant occurredAt) {
		repository.confirmBusinessFailure(token, code, message, occurredAt);
		return new ConfirmedOrderBusinessException(code, message, occurredAt);
	}

	private String fingerprint(List<OrderLineCommand> lines) {
		String canonical = lines.stream()
				.sorted(Comparator.comparing(OrderLineCommand::menuId))
				.map(line -> line.menuId() + ":" + line.quantity())
				.collect(Collectors.joining("|"));
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
					.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}
}
