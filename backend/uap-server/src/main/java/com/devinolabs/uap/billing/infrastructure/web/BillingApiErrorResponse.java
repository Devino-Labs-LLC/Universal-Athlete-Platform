package com.devinolabs.uap.billing.infrastructure.web;

import java.time.Instant;
import java.util.List;

record BillingApiErrorResponse(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<FieldErrorDetail> fieldErrors) {

	record FieldErrorDetail(String field, String message) {
	}
}
