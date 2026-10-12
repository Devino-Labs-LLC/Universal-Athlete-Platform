package com.devinolabs.uap.aicoach.infrastructure.web;

import java.time.Instant;
import java.util.List;

public record CoachApiError(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<FieldErrorDetail> details) {

	public record FieldErrorDetail(String field, String message) {
	}

}
