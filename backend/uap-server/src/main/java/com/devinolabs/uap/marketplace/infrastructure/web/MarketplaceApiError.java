package com.devinolabs.uap.marketplace.infrastructure.web;

import java.time.Instant;
import java.util.List;

public record MarketplaceApiError(
		String code,
		String message,
		Instant timestamp,
		String path,
		List<String> details) {
}
