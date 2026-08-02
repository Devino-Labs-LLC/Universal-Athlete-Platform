package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.training.application.GetBodyAreaDiscomfortHistoryUseCase;
import com.devinolabs.uap.training.application.GetRecoveryBaselineDashboardUseCase;
import com.devinolabs.uap.training.application.GetRecoveryMetricTrendUseCase;
import com.devinolabs.uap.training.application.InvalidRecoveryMetricTypeException;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

@RestController
class RecoveryAnalyticsController {

	private final GetRecoveryMetricTrendUseCase getRecoveryMetricTrendUseCase;
	private final GetRecoveryBaselineDashboardUseCase getRecoveryBaselineDashboardUseCase;
	private final GetBodyAreaDiscomfortHistoryUseCase getBodyAreaDiscomfortHistoryUseCase;

	RecoveryAnalyticsController(
			GetRecoveryMetricTrendUseCase getRecoveryMetricTrendUseCase,
			GetRecoveryBaselineDashboardUseCase getRecoveryBaselineDashboardUseCase,
			GetBodyAreaDiscomfortHistoryUseCase getBodyAreaDiscomfortHistoryUseCase) {
		this.getRecoveryMetricTrendUseCase = Objects.requireNonNull(getRecoveryMetricTrendUseCase);
		this.getRecoveryBaselineDashboardUseCase = Objects.requireNonNull(getRecoveryBaselineDashboardUseCase);
		this.getBodyAreaDiscomfortHistoryUseCase = Objects.requireNonNull(getBodyAreaDiscomfortHistoryUseCase);
	}

	@GetMapping("/api/v1/training/recovery-analytics/trends/{metricType}")
	RecoveryMetricTrendResponse trend(
			@PathVariable String metricType,
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(defaultValue = "false") boolean includeTrainingLoad,
			org.springframework.security.core.Authentication authentication) {
		RecoveryMetricType resolvedMetricType = parseMetricType(metricType);
		return RecoveryMetricTrendResponse.from(getRecoveryMetricTrendUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				resolvedMetricType,
				startDate,
				endDate,
				includeTrainingLoad));
	}

	@GetMapping("/api/v1/training/recovery-analytics/dashboard")
	RecoveryBaselineDashboardResponse dashboard(
			@RequestParam(required = false) LocalDate targetDate,
			@RequestParam int baselineWindowDays,
			@RequestParam(defaultValue = "false") boolean includeTrainingLoad,
			org.springframework.security.core.Authentication authentication) {
		return RecoveryBaselineDashboardResponse.from(getRecoveryBaselineDashboardUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				targetDate,
				baselineWindowDays,
				includeTrainingLoad));
	}

	@GetMapping("/api/v1/training/recovery-analytics/discomfort-history")
	BodyAreaDiscomfortHistoryResponse discomfortHistory(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(required = false) BodyArea bodyArea,
			@RequestParam(required = false) BodySide bodySide,
			org.springframework.security.core.Authentication authentication) {
		return BodyAreaDiscomfortHistoryResponse.from(getBodyAreaDiscomfortHistoryUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				startDate,
				endDate,
				bodyArea,
				bodySide));
	}

	private static RecoveryMetricType parseMetricType(String metricType) {
		try {
			return RecoveryMetricType.valueOf(metricType.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidRecoveryMetricTypeException("Unsupported recovery metric type: " + metricType);
		}
	}

}
