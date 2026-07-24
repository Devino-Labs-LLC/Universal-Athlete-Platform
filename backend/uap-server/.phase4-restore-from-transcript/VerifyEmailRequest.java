package com.devinolabs.uap.identity.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record VerifyEmailRequest(
		@NotBlank String token) {
}
