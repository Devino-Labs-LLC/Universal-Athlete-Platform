package com.devinolabs.uap.identity.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
		@NotBlank String email,
		@NotBlank String password) {
}
