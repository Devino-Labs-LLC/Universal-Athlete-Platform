package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<FieldErrorDetail> details) {

	public record FieldErrorDetail(String field, String message) {
	}

}
