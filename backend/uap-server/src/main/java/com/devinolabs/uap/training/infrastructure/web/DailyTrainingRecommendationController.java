package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.training.application.CompareDailyTrainingRecommendationsUseCase;
import com.devinolabs.uap.training.application.GenerateCurrentDailyTrainingRecommendationUseCase;
import com.devinolabs.uap.training.application.GenerateDailyTrainingRecommendationUseCase;
import com.devinolabs.uap.training.application.GenerateRecommendedWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.GetDailyTrainingRecommendationHistoryUseCase;
import com.devinolabs.uap.training.application.GetDailyTrainingRecommendationUseCase;

@RestController
@RequestMapping("/api/v1/training/recommendations")
class DailyTrainingRecommendationController {

	private final GenerateDailyTrainingRecommendationUseCase generateUseCase;
	private final GenerateCurrentDailyTrainingRecommendationUseCase generateCurrentUseCase;
	private final GetDailyTrainingRecommendationUseCase getUseCase;
	private final GetDailyTrainingRecommendationHistoryUseCase historyUseCase;
	private final CompareDailyTrainingRecommendationsUseCase compareUseCase;
	private final GenerateRecommendedWorkoutAdaptationProposalUseCase generateRecommendedAdaptationUseCase;

	DailyTrainingRecommendationController(
			GenerateDailyTrainingRecommendationUseCase generateUseCase,
			GenerateCurrentDailyTrainingRecommendationUseCase generateCurrentUseCase,
			GetDailyTrainingRecommendationUseCase getUseCase,
			GetDailyTrainingRecommendationHistoryUseCase historyUseCase,
			CompareDailyTrainingRecommendationsUseCase compareUseCase,
			GenerateRecommendedWorkoutAdaptationProposalUseCase generateRecommendedAdaptationUseCase) {
		this.generateUseCase = Objects.requireNonNull(generateUseCase);
		this.generateCurrentUseCase = Objects.requireNonNull(generateCurrentUseCase);
		this.getUseCase = Objects.requireNonNull(getUseCase);
		this.historyUseCase = Objects.requireNonNull(historyUseCase);
		this.compareUseCase = Objects.requireNonNull(compareUseCase);
		this.generateRecommendedAdaptationUseCase = Objects.requireNonNull(generateRecommendedAdaptationUseCase);
	}

	@PostMapping
	DailyTrainingRecommendationResponse generate(
			@Valid @RequestBody GenerateDailyTrainingRecommendationRequest request,
			org.springframework.security.core.Authentication authentication) {
		return DailyTrainingRecommendationResponse.from(generateUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				request.dailyReadinessAssessmentId()));
	}

	@PostMapping("/daily/{date}")
	DailyTrainingRecommendationResponse generateForDate(
			@PathVariable LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return DailyTrainingRecommendationResponse.from(generateCurrentUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date));
	}

	@GetMapping("/history")
	DailyTrainingRecommendationHistoryResponse history(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(defaultValue = "true") boolean currentSnapshotOnly,
			@RequestParam(required = false) String algorithmVersion,
			@RequestParam(required = false) String overallAction,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			org.springframework.security.core.Authentication authentication) {
		return DailyTrainingRecommendationHistoryResponse.from(historyUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				startDate,
				endDate,
				currentSnapshotOnly,
				algorithmVersion,
				overallAction,
				page,
				size));
	}

	@GetMapping("/compare")
	DailyTrainingRecommendationComparisonResponse compare(
			@RequestParam UUID olderRecommendationId,
			@RequestParam UUID newerRecommendationId,
			org.springframework.security.core.Authentication authentication) {
		return DailyTrainingRecommendationComparisonResponse.from(compareUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				olderRecommendationId,
				newerRecommendationId));
	}

	@GetMapping("/{recommendationId}")
	DailyTrainingRecommendationResponse get(
			@PathVariable UUID recommendationId,
			org.springframework.security.core.Authentication authentication) {
		return DailyTrainingRecommendationResponse.from(getUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				recommendationId));
	}

	@PostMapping("/{recommendationId}/occurrences/{occurrenceId}/adaptation-proposals")
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutAdaptationProposalResponse generateRecommendedAdaptationProposal(
			@PathVariable UUID recommendationId,
			@PathVariable UUID occurrenceId,
			@RequestBody(required = false) GenerateWorkoutAdaptationProposalRequest request,
			org.springframework.security.core.Authentication authentication) {
		GenerateWorkoutAdaptationProposalRequest resolved = request == null
				? new GenerateWorkoutAdaptationProposalRequest(null, null, null)
				: request;
		return WorkoutAdaptationProposalResponse.from(generateRecommendedAdaptationUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				recommendationId,
				occurrenceId,
				resolved.suggestionLimit(),
				resolved.includeAlternatives(),
				resolved.expirationMinutes()));
	}

}
