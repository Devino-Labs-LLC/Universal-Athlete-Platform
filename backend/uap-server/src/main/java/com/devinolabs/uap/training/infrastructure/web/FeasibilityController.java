package com.devinolabs.uap.training.infrastructure.web;

import java.util.Objects;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.AnalyzeTrainingPlanFeasibilityUseCase;
import com.devinolabs.uap.training.application.AnalyzeWorkoutDayFeasibilityUseCase;
import com.devinolabs.uap.training.application.AnalyzeWorkoutOccurrenceFeasibilityUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
@RequestMapping("/api/v1/training/plans")
class FeasibilityController {

	private final AnalyzeWorkoutDayFeasibilityUseCase analyzeWorkoutDayFeasibilityUseCase;
	private final AnalyzeTrainingPlanFeasibilityUseCase analyzeTrainingPlanFeasibilityUseCase;
	private final AnalyzeWorkoutOccurrenceFeasibilityUseCase analyzeWorkoutOccurrenceFeasibilityUseCase;

	FeasibilityController(
			AnalyzeWorkoutDayFeasibilityUseCase analyzeWorkoutDayFeasibilityUseCase,
			AnalyzeTrainingPlanFeasibilityUseCase analyzeTrainingPlanFeasibilityUseCase,
			AnalyzeWorkoutOccurrenceFeasibilityUseCase analyzeWorkoutOccurrenceFeasibilityUseCase) {
		this.analyzeWorkoutDayFeasibilityUseCase = Objects.requireNonNull(analyzeWorkoutDayFeasibilityUseCase);
		this.analyzeTrainingPlanFeasibilityUseCase = Objects.requireNonNull(analyzeTrainingPlanFeasibilityUseCase);
		this.analyzeWorkoutOccurrenceFeasibilityUseCase = Objects.requireNonNull(analyzeWorkoutOccurrenceFeasibilityUseCase);
	}

	@GetMapping("/{planId}/days/{dayId}/feasibility")
	WorkoutDayFeasibilityResponse analyzeDay(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@RequestParam UUID trainingEnvironmentId,
			@RequestParam(required = false) Integer suggestionLimit,
			@RequestParam(required = false) Boolean includeAlternatives,
			Authentication authentication) {
		return WorkoutDayFeasibilityResponse.from(analyzeWorkoutDayFeasibilityUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				TrainingEnvironmentId.of(trainingEnvironmentId),
				suggestionLimit,
				includeAlternatives));
	}

	@GetMapping("/{planId}/feasibility")
	TrainingPlanFeasibilityResponse analyzePlan(
			@PathVariable UUID planId,
			@RequestParam(required = false) UUID trainingEnvironmentId,
			@RequestParam(required = false) Boolean usePreferredEnvironments,
			@RequestParam(required = false) Integer suggestionLimit,
			@RequestParam(required = false) Boolean includeAlternatives,
			Authentication authentication) {
		return TrainingPlanFeasibilityResponse.from(analyzeTrainingPlanFeasibilityUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				trainingEnvironmentId == null ? null : TrainingEnvironmentId.of(trainingEnvironmentId),
				usePreferredEnvironments,
				suggestionLimit,
				includeAlternatives));
	}

	@GetMapping("/{planId}/days/{dayId}/occurrences/{occurrenceId}/feasibility")
	WorkoutOccurrenceFeasibilityResponse analyzeOccurrence(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@RequestParam(required = false) Integer suggestionLimit,
			@RequestParam(required = false) Boolean includeAlternatives,
			Authentication authentication) {
		return WorkoutOccurrenceFeasibilityResponse.from(analyzeWorkoutOccurrenceFeasibilityUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				suggestionLimit,
				includeAlternatives));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return AccountId.of(principal.accountUuid());
	}

}
