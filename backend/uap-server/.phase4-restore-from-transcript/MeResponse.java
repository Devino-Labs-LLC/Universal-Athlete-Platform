package com.devinolabs.uap.identity.infrastructure.web;

import java.time.Instant;

public record MeResponse(
		String accountId,
		String email,
		String status,
		Instant emailVerifiedAt) {
}
