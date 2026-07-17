package io.github.w00lam.coffeeorderservice.web;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

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
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(code, message, List.of(), Instant.now()));
	}
}
