package com.devinolabs.uap.sponsorship.infrastructure.web;

import java.time.Instant;
import java.util.List;

public record SponsorshipApiError(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<FieldErrorDetail> details) {

	public record FieldErrorDetail(String field, String message) {
	}

}
