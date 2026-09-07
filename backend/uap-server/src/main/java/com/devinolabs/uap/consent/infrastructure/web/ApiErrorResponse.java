package com.devinolabs.uap.consent.infrastructure.web;

import java.time.Instant;
import java.util.List;

record ApiErrorResponse(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<FieldErrorDetail> details) {

	record FieldErrorDetail(String field, String message) {
	}

}
