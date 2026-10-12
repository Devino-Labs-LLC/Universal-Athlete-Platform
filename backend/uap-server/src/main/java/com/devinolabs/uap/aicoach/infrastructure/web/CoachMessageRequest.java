package com.devinolabs.uap.aicoach.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CoachMessageRequest(
		@NotBlank @Size(max = 2000) String text) {
}
