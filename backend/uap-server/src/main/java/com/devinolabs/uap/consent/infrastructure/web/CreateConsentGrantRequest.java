package com.devinolabs.uap.consent.infrastructure.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

record CreateConsentGrantRequest(
		@NotNull(message = "teamId is required") UUID teamId,
		@NotEmpty(message = "scopes must not be empty") List<String> scopes) {
}
