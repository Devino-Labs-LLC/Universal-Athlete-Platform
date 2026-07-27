package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateExerciseDefinitionRequest(
		@NotBlank @Size(min = 2, max = 150) String canonicalName) {
}
