package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BodyAreaDiscomfortRequest(
		@NotBlank String bodyArea,
		@NotBlank String side,
		@Min(1) @Max(5) int intensity,
		@Size(max = 250) String notes) {
}
