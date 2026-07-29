package com.devinolabs.uap.training.infrastructure.web;

import jakarta.validation.constraints.Size;

record RevertWorkoutExerciseExecutionSubstitutionRequest(
		@Size(max = 2000) String notes) {
}
