package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
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

import com.devinolabs.uap.training.application.CompareDailyAthleteStateSnapshotsUseCase;
import com.devinolabs.uap.training.application.GenerateDailyAthleteStateSnapshotUseCase;
import com.devinolabs.uap.training.application.GetCurrentDailyAthleteStateSnapshotUseCase;
import com.devinolabs.uap.training.application.GetDailyAthleteStateHistoryUseCase;
import com.devinolabs.uap.training.application.GetDailyAthleteStateSnapshotUseCase;
import com.devinolabs.uap.training.application.ListDailyAthleteStateSnapshotVersionsUseCase;
import com.devinolabs.uap.training.application.RegenerateDailyAthleteStateSnapshotUseCase;

@RestController
@RequestMapping("/api/v1/training/athlete-state")
class DailyAthleteStateController {

	private final GenerateDailyAthleteStateSnapshotUseCase generateUseCase;
	private final RegenerateDailyAthleteStateSnapshotUseCase regenerateUseCase;
	private final GetCurrentDailyAthleteStateSnapshotUseCase getCurrentUseCase;
	private final GetDailyAthleteStateSnapshotUseCase getByIdUseCase;
	private final ListDailyAthleteStateSnapshotVersionsUseCase listVersionsUseCase;
	private final GetDailyAthleteStateHistoryUseCase historyUseCase;
	private final CompareDailyAthleteStateSnapshotsUseCase compareUseCase;

	DailyAthleteStateController(
			GenerateDailyAthleteStateSnapshotUseCase generateUseCase,
			RegenerateDailyAthleteStateSnapshotUseCase regenerateUseCase,
			GetCurrentDailyAthleteStateSnapshotUseCase getCurrentUseCase,
			GetDailyAthleteStateSnapshotUseCase getByIdUseCase,
			ListDailyAthleteStateSnapshotVersionsUseCase listVersionsUseCase,
			GetDailyAthleteStateHistoryUseCase historyUseCase,
			CompareDailyAthleteStateSnapshotsUseCase compareUseCase) {
		this.generateUseCase = Objects.requireNonNull(generateUseCase);
		this.regenerateUseCase = Objects.requireNonNull(regenerateUseCase);
		this.getCurrentUseCase = Objects.requireNonNull(getCurrentUseCase);
		this.getByIdUseCase = Objects.requireNonNull(getByIdUseCase);
		this.listVersionsUseCase = Objects.requireNonNull(listVersionsUseCase);
		this.historyUseCase = Objects.requireNonNull(historyUseCase);
		this.compareUseCase = Objects.requireNonNull(compareUseCase);
	}

	@PostMapping("/daily/{date}")
	@ResponseStatus(HttpStatus.OK)
	DailyAthleteStateSnapshotResponse generate(
			@PathVariable LocalDate date,
			@Valid @RequestBody GenerateDailyAthleteStateSnapshotRequest request,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateSnapshotResponse.from(generateUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date,
				request.baselineWindowDays()));
	}

	@PostMapping("/daily/{date}/regenerate")
	DailyAthleteStateSnapshotResponse regenerate(
			@PathVariable LocalDate date,
			@Valid @RequestBody GenerateDailyAthleteStateSnapshotRequest request,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateSnapshotResponse.from(regenerateUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date,
				request.baselineWindowDays()));
	}

	@GetMapping("/daily/{date}")
	DailyAthleteStateSnapshotResponse getCurrent(
			@PathVariable LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateSnapshotResponse.from(getCurrentUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				date));
	}

	@GetMapping("/snapshots/{snapshotId}")
	DailyAthleteStateSnapshotResponse getById(
			@PathVariable UUID snapshotId,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateSnapshotResponse.from(getByIdUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				snapshotId));
	}

	@GetMapping("/daily/{date}/versions")
	List<DailyAthleteStateSnapshotVersionResponse> versions(
			@PathVariable LocalDate date,
			org.springframework.security.core.Authentication authentication) {
		return listVersionsUseCase.execute(RecoveryAnalyticsWebSupport.accountId(authentication), date).stream()
				.map(DailyAthleteStateSnapshotVersionResponse::from)
				.toList();
	}

	@GetMapping("/history")
	DailyAthleteStateHistoryResponse history(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(defaultValue = "true") boolean currentOnly,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateHistoryResponse.from(historyUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				startDate,
				endDate,
				currentOnly,
				page,
				size));
	}

	@GetMapping("/snapshots/compare")
	DailyAthleteStateSnapshotComparisonResponse compare(
			@RequestParam UUID olderSnapshotId,
			@RequestParam UUID newerSnapshotId,
			org.springframework.security.core.Authentication authentication) {
		return DailyAthleteStateSnapshotComparisonResponse.from(compareUseCase.execute(
				RecoveryAnalyticsWebSupport.accountId(authentication),
				olderSnapshotId,
				newerSnapshotId));
	}

}
