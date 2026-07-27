package com.devinolabs.uap.training.infrastructure.web;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.GetWorkoutOccurrencePerformanceSummaryUseCase;
import com.devinolabs.uap.training.application.RecomputeWorkoutExerciseExecutionMetricsUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}")
class WorkoutOccurrencePerformanceController {

	private final GetWorkoutOccurrencePerformanceSummaryUseCase getWorkoutOccurrencePerformanceSummaryUseCase;
	private final RecomputeWorkoutExerciseExecutionMetricsUseCase recomputeWorkoutExerciseExecutionMetricsUseCase;

	WorkoutOccurrencePerformanceController(
			GetWorkoutOccurrencePerformanceSummaryUseCase getWorkoutOccurrencePerformanceSummaryUseCase,
			RecomputeWorkoutExerciseExecutionMetricsUseCase recomputeWorkoutExerciseExecutionMetricsUseCase) {
		this.getWorkoutOccurrencePerformanceSummaryUseCase =
				Objects.requireNonNull(getWorkoutOccurrencePerformanceSummaryUseCase);
		this.recomputeWorkoutExerciseExecutionMetricsUseCase =
				Objects.requireNonNull(recomputeWorkoutExerciseExecutionMetricsUseCase);
	}

	@GetMapping("/performance")
	WorkoutOccurrencePerformanceResponse performance(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return WorkoutOccurrencePerformanceResponse.from(getWorkoutOccurrencePerformanceSummaryUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/exercises/{executionId}/performance/recompute")
	ExerciseExecutionPerformanceResponse recompute(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return ExerciseExecutionPerformanceResponse.from(recomputeWorkoutExerciseExecutionMetricsUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId)));
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}
