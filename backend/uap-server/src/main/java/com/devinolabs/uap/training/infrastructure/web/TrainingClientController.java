package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.training.application.GetRecoveryOverviewUseCase;
import com.devinolabs.uap.training.application.GetTrainingClientBootstrapUseCase;
import com.devinolabs.uap.training.application.GetTrainingOverviewUseCase;
import com.devinolabs.uap.training.application.GetTrainingTodayDashboardUseCase;
import com.devinolabs.uap.training.application.GetWorkoutLaunchContextUseCase;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
@RequestMapping("/api/v1/training/client")
class TrainingClientController {

	private final GetTrainingTodayDashboardUseCase getTrainingTodayDashboardUseCase;
	private final GetTrainingOverviewUseCase getTrainingOverviewUseCase;
	private final GetRecoveryOverviewUseCase getRecoveryOverviewUseCase;
	private final GetWorkoutLaunchContextUseCase getWorkoutLaunchContextUseCase;
	private final GetTrainingClientBootstrapUseCase getTrainingClientBootstrapUseCase;

	TrainingClientController(
			GetTrainingTodayDashboardUseCase getTrainingTodayDashboardUseCase,
			GetTrainingOverviewUseCase getTrainingOverviewUseCase,
			GetRecoveryOverviewUseCase getRecoveryOverviewUseCase,
			GetWorkoutLaunchContextUseCase getWorkoutLaunchContextUseCase,
			GetTrainingClientBootstrapUseCase getTrainingClientBootstrapUseCase) {
		this.getTrainingTodayDashboardUseCase = Objects.requireNonNull(getTrainingTodayDashboardUseCase);
		this.getTrainingOverviewUseCase = Objects.requireNonNull(getTrainingOverviewUseCase);
		this.getRecoveryOverviewUseCase = Objects.requireNonNull(getRecoveryOverviewUseCase);
		this.getWorkoutLaunchContextUseCase = Objects.requireNonNull(getWorkoutLaunchContextUseCase);
		this.getTrainingClientBootstrapUseCase = Objects.requireNonNull(getTrainingClientBootstrapUseCase);
	}

	@GetMapping("/today")
	TrainingTodayDashboardResponse today(
			@RequestParam(required = false) LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return TrainingTodayDashboardResponse.from(getTrainingTodayDashboardUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date));
	}

	@GetMapping("/training-overview")
	TrainingOverviewResponse trainingOverview(
			@RequestParam(required = false) LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return TrainingOverviewResponse.from(getTrainingOverviewUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date));
	}

	@GetMapping("/recovery-overview")
	RecoveryOverviewResponse recoveryOverview(
			@RequestParam(required = false) LocalDate date,
			@RequestParam(required = false) Integer trendDays,
			org.springframework.security.core.Authentication authentication) {
		return RecoveryOverviewResponse.from(getRecoveryOverviewUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date,
				trendDays));
	}

	@GetMapping("/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/launch-context")
	WorkoutLaunchContextResponse launchContext(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			org.springframework.security.core.Authentication authentication) {
		return WorkoutLaunchContextResponse.from(getWorkoutLaunchContextUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@GetMapping("/bootstrap")
	TrainingClientBootstrapResponse bootstrap(
			org.springframework.security.core.Authentication authentication) {
		return TrainingClientBootstrapResponse.from(getTrainingClientBootstrapUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication)));
	}

}
