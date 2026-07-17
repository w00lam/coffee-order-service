package io.github.w00lam.coffeeorderservice.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		String code,
		String message,
		List<ApiErrorDetail> details,
		Instant occurredAt) {

	public record ApiErrorDetail(String field, String reason) {
	}
}
