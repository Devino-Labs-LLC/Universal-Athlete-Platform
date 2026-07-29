package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;

record SubstituteWorkoutExerciseExecutionRequest(
		@NotNull UUID exerciseDefinitionId,
		@NotNull ExerciseSubstitutionReason reason,
		@Size(max = 2000) String notes) {
}
