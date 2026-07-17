package io.github.w00lam.coffeeorderservice.web;

import io.github.w00lam.coffeeorderservice.auth.application.UnauthenticatedException;
import io.github.w00lam.coffeeorderservice.auth.application.UserNotFoundException;
import io.github.w00lam.coffeeorderservice.order.application.ConfirmedOrderBusinessException;
import io.github.w00lam.coffeeorderservice.order.application.OrderTokenExpiredException;
import io.github.w00lam.coffeeorderservice.order.application.OrderTokenNotFoundException;
import io.github.w00lam.coffeeorderservice.order.application.OrderTokenRequestConflictException;
import io.github.w00lam.coffeeorderservice.order.web.OrderRequestValidationException;
import io.github.w00lam.coffeeorderservice.point.application.PointBalanceOutOfRangeException;
import io.github.w00lam.coffeeorderservice.point.web.InvalidChargeAmountException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(InvalidRequestException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidRequest(InvalidRequestException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(InvalidChargeAmountException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidChargeAmount(InvalidChargeAmountException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_CHARGE_AMOUNT", "충전금액은 1 이상의 정수여야 합니다.");
	}

	@ExceptionHandler(UnauthenticatedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
		return error(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증된 사용자 정보가 필요합니다.");
	}

	@ExceptionHandler(UserNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다.");
	}

	@ExceptionHandler(PointBalanceOutOfRangeException.class)
	public ResponseEntity<ApiErrorResponse> handlePointBalanceOutOfRange(PointBalanceOutOfRangeException exception) {
		return error(HttpStatus.UNPROCESSABLE_CONTENT, "POINT_BALANCE_OUT_OF_RANGE", "충전 후 잔액이 허용 범위를 초과합니다.");
	}

	@ExceptionHandler(OrderRequestValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleOrderValidation(OrderRequestValidationException exception) {
		return error(HttpStatus.BAD_REQUEST, exception.code(), exception.getMessage());
	}

	@ExceptionHandler(OrderTokenNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleOrderTokenNotFound(OrderTokenNotFoundException exception) {
		return error(HttpStatus.NOT_FOUND, "ORDER_TOKEN_NOT_FOUND", "주문 토큰을 찾을 수 없습니다.");
	}

	@ExceptionHandler(OrderTokenRequestConflictException.class)
	public ResponseEntity<ApiErrorResponse> handleOrderTokenConflict(OrderTokenRequestConflictException exception) {
		return error(HttpStatus.CONFLICT, "ORDER_TOKEN_REQUEST_CONFLICT", "주문 토큰의 요청 내용이 충돌합니다.");
	}

	@ExceptionHandler(OrderTokenExpiredException.class)
	public ResponseEntity<ApiErrorResponse> handleOrderTokenExpired(OrderTokenExpiredException exception) {
		return error(HttpStatus.GONE, "ORDER_TOKEN_EXPIRED", "주문 토큰이 만료되었습니다.");
	}

	@ExceptionHandler(ConfirmedOrderBusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleConfirmedOrderFailure(ConfirmedOrderBusinessException exception) {
		return error(HttpStatus.UNPROCESSABLE_CONTENT, exception.code(), exception.getMessage(), exception.occurredAt());
	}

	@ExceptionHandler(DataAccessResourceFailureException.class)
	public ResponseEntity<ApiErrorResponse> handleUnavailable(DataAccessResourceFailureException exception) {
		log.error("Database is temporarily unavailable", exception);
		return error(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "서비스를 일시적으로 사용할 수 없습니다.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleInternal(Exception exception) {
		log.error("Unexpected server error", exception);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message) {
		return error(status, code, message, Instant.now());
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String code, String message, Instant occurredAt) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(code, message, List.of(), occurredAt));
	}
}
