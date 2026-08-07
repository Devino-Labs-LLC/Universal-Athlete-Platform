package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.training.application.CompareDailyReadinessAssessmentsUseCase;
import com.devinolabs.uap.training.application.GenerateCurrentDailyReadinessAssessmentUseCase;
import com.devinolabs.uap.training.application.GenerateDailyReadinessAssessmentUseCase;
import com.devinolabs.uap.training.application.GetDailyReadinessAssessmentUseCase;
import com.devinolabs.uap.training.application.GetDailyReadinessHistoryUseCase;

@RestController
@RequestMapping("/api/v1/training/readiness")
class DailyReadinessController {

	private final GenerateDailyReadinessAssessmentUseCase generateUseCase;
	private final GenerateCurrentDailyReadinessAssessmentUseCase generateCurrentUseCase;
	private final GetDailyReadinessAssessmentUseCase getUseCase;
	private final GetDailyReadinessHistoryUseCase historyUseCase;
	private final CompareDailyReadinessAssessmentsUseCase compareUseCase;

	DailyReadinessController(
			GenerateDailyReadinessAssessmentUseCase generateUseCase,
			GenerateCurrentDailyReadinessAssessmentUseCase generateCurrentUseCase,
			GetDailyReadinessAssessmentUseCase getUseCase,
			GetDailyReadinessHistoryUseCase historyUseCase,
			CompareDailyReadinessAssessmentsUseCase compareUseCase) {
		this.generateUseCase = Objects.requireNonNull(generateUseCase);
		this.generateCurrentUseCase = Objects.requireNonNull(generateCurrentUseCase);
		this.getUseCase = Objects.requireNonNull(getUseCase);
		this.historyUseCase = Objects.requireNonNull(historyUseCase);
		this.compareUseCase = Objects.requireNonNull(compareUseCase);
	}

	@PostMapping("/assessments")
	DailyReadinessAssessmentResponse generate(
			@Valid @RequestBody GenerateDailyReadinessAssessmentRequest request,
			org.springframework.security.core.Authentication authentication) {
		return DailyReadinessAssessmentResponse.from(generateUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				request.dailyAthleteStateSnapshotId()));
	}

	@PostMapping("/daily/{date}")
	DailyReadinessAssessmentResponse generateForDate(
			@PathVariable LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return DailyReadinessAssessmentResponse.from(generateCurrentUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date));
	}

	@GetMapping("/assessments/{assessmentId}")
	DailyReadinessAssessmentResponse get(
			@PathVariable UUID assessmentId,
			org.springframework.security.core.Authentication authentication) {
		return DailyReadinessAssessmentResponse.from(getUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				assessmentId));
	}

	@GetMapping("/history")
	DailyReadinessHistoryResponse history(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(defaultValue = "true") boolean currentSnapshotOnly,
			@RequestParam(required = false) String algorithmVersion,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			org.springframework.security.core.Authentication authentication) {
		return DailyReadinessHistoryResponse.from(historyUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				startDate,
				endDate,
				currentSnapshotOnly,
				algorithmVersion,
				page,
				size));
	}

	@GetMapping("/assessments/compare")
	DailyReadinessAssessmentComparisonResponse compare(
			@RequestParam UUID olderAssessmentId,
			@RequestParam UUID newerAssessmentId,
			org.springframework.security.core.Authentication authentication) {
		return DailyReadinessAssessmentComparisonResponse.from(compareUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				olderAssessmentId,
				newerAssessmentId));
	}

}
