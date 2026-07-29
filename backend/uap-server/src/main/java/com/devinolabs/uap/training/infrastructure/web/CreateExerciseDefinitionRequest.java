package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateExerciseDefinitionRequest(
		@NotBlank @Size(min = 2, max = 150) String canonicalName,
		@NotNull @Valid ExerciseDefinitionMetadataRequest metadata) {
}
