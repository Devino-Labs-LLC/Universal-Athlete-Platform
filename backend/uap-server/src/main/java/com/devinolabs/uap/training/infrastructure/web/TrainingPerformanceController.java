package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.GetAthleteExercisePerformanceHistoryUseCase;
import com.devinolabs.uap.training.application.GetAthleteExercisePersonalRecordsUseCase;
import com.devinolabs.uap.training.application.GetRecentAthletePersonalRecordsUseCase;
import com.devinolabs.uap.training.application.ListAthletePersonalRecordsUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordType;

@RestController
@RequestMapping("/api/v1/training/performance")
class TrainingPerformanceController {

	private final GetAthleteExercisePerformanceHistoryUseCase getAthleteExercisePerformanceHistoryUseCase;
	private final ListAthletePersonalRecordsUseCase listAthletePersonalRecordsUseCase;
	private final GetAthleteExercisePersonalRecordsUseCase getAthleteExercisePersonalRecordsUseCase;
	private final GetRecentAthletePersonalRecordsUseCase getRecentAthletePersonalRecordsUseCase;

	TrainingPerformanceController(
			GetAthleteExercisePerformanceHistoryUseCase getAthleteExercisePerformanceHistoryUseCase,
			ListAthletePersonalRecordsUseCase listAthletePersonalRecordsUseCase,
			GetAthleteExercisePersonalRecordsUseCase getAthleteExercisePersonalRecordsUseCase,
			GetRecentAthletePersonalRecordsUseCase getRecentAthletePersonalRecordsUseCase) {
		this.getAthleteExercisePerformanceHistoryUseCase =
				Objects.requireNonNull(getAthleteExercisePerformanceHistoryUseCase);
		this.listAthletePersonalRecordsUseCase = Objects.requireNonNull(listAthletePersonalRecordsUseCase);
		this.getAthleteExercisePersonalRecordsUseCase =
				Objects.requireNonNull(getAthleteExercisePersonalRecordsUseCase);
		this.getRecentAthletePersonalRecordsUseCase = Objects.requireNonNull(getRecentAthletePersonalRecordsUseCase);
	}

	@GetMapping("/exercises/{exercisePerformanceKey}")
	AthleteExercisePerformanceHistoryResponse history(
			@PathVariable UUID exercisePerformanceKey,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledTo,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return AthleteExercisePerformanceHistoryResponse.from(getAthleteExercisePerformanceHistoryUseCase.execute(
				accountId(authentication),
				ExercisePerformanceKey.of(exercisePerformanceKey),
				scheduledFrom,
				scheduledTo,
				page,
				size));
	}

	@GetMapping("/exercises/{exercisePerformanceKey}/personal-records")
	List<PersonalRecordResponse> exercisePersonalRecords(
			@PathVariable UUID exercisePerformanceKey,
			Authentication authentication) {
		return getAthleteExercisePersonalRecordsUseCase
				.execute(accountId(authentication), ExercisePerformanceKey.of(exercisePerformanceKey))
				.stream()
				.map(PersonalRecordResponse::from)
				.toList();
	}

	@GetMapping("/personal-records")
	List<PersonalRecordResponse> personalRecords(
			@RequestParam(required = false) UUID exercisePerformanceKey,
			@RequestParam(required = false) PersonalRecordType recordType,
			Authentication authentication) {
		return listAthletePersonalRecordsUseCase
				.execute(
						accountId(authentication),
						exercisePerformanceKey == null ? null : ExercisePerformanceKey.of(exercisePerformanceKey),
						recordType)
				.stream()
				.map(PersonalRecordResponse::from)
				.toList();
	}

	@GetMapping("/personal-records/recent")
	List<PersonalRecordResponse> recentPersonalRecords(
			@RequestParam(required = false) Integer days,
			@RequestParam(required = false) Integer limit,
			Authentication authentication) {
		return getRecentAthletePersonalRecordsUseCase
				.execute(accountId(authentication), days, limit)
				.stream()
				.map(PersonalRecordResponse::from)
				.toList();
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}
