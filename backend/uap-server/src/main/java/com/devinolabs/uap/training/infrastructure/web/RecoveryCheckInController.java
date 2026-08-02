package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CompareDailyRecoveryCheckInToBaselineUseCase;
import com.devinolabs.uap.training.application.CompareRecoveryDateToBaselineUseCase;
import com.devinolabs.uap.training.application.CreateDailyRecoveryCheckInUseCase;
import com.devinolabs.uap.training.application.GetAthleteRecoveryHistoryUseCase;
import com.devinolabs.uap.training.application.GetDailyRecoveryCheckInByDateUseCase;
import com.devinolabs.uap.training.application.GetDailyRecoveryCheckInUseCase;
import com.devinolabs.uap.training.application.GetRecoveryCheckInCalendarUseCase;
import com.devinolabs.uap.training.application.ListDailyRecoveryCheckInRevisionsUseCase;
import com.devinolabs.uap.training.application.ListDailyRecoveryCheckInsUseCase;
import com.devinolabs.uap.training.application.UpdateDailyRecoveryCheckInCommand;
import com.devinolabs.uap.training.application.UpdateDailyRecoveryCheckInUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

@RestController
class RecoveryCheckInController {

	private final CreateDailyRecoveryCheckInUseCase createDailyRecoveryCheckInUseCase;
	private final UpdateDailyRecoveryCheckInUseCase updateDailyRecoveryCheckInUseCase;
	private final GetDailyRecoveryCheckInUseCase getDailyRecoveryCheckInUseCase;
	private final GetDailyRecoveryCheckInByDateUseCase getDailyRecoveryCheckInByDateUseCase;
	private final ListDailyRecoveryCheckInsUseCase listDailyRecoveryCheckInsUseCase;
	private final ListDailyRecoveryCheckInRevisionsUseCase listDailyRecoveryCheckInRevisionsUseCase;
	private final GetRecoveryCheckInCalendarUseCase getRecoveryCheckInCalendarUseCase;
	private final GetAthleteRecoveryHistoryUseCase getAthleteRecoveryHistoryUseCase;
	private final CompareDailyRecoveryCheckInToBaselineUseCase compareDailyRecoveryCheckInToBaselineUseCase;
	private final CompareRecoveryDateToBaselineUseCase compareRecoveryDateToBaselineUseCase;

	RecoveryCheckInController(
			CreateDailyRecoveryCheckInUseCase createDailyRecoveryCheckInUseCase,
			UpdateDailyRecoveryCheckInUseCase updateDailyRecoveryCheckInUseCase,
			GetDailyRecoveryCheckInUseCase getDailyRecoveryCheckInUseCase,
			GetDailyRecoveryCheckInByDateUseCase getDailyRecoveryCheckInByDateUseCase,
			ListDailyRecoveryCheckInsUseCase listDailyRecoveryCheckInsUseCase,
			ListDailyRecoveryCheckInRevisionsUseCase listDailyRecoveryCheckInRevisionsUseCase,
			GetRecoveryCheckInCalendarUseCase getRecoveryCheckInCalendarUseCase,
			GetAthleteRecoveryHistoryUseCase getAthleteRecoveryHistoryUseCase,
			CompareDailyRecoveryCheckInToBaselineUseCase compareDailyRecoveryCheckInToBaselineUseCase,
			CompareRecoveryDateToBaselineUseCase compareRecoveryDateToBaselineUseCase) {
		this.createDailyRecoveryCheckInUseCase = Objects.requireNonNull(createDailyRecoveryCheckInUseCase);
		this.updateDailyRecoveryCheckInUseCase = Objects.requireNonNull(updateDailyRecoveryCheckInUseCase);
		this.getDailyRecoveryCheckInUseCase = Objects.requireNonNull(getDailyRecoveryCheckInUseCase);
		this.getDailyRecoveryCheckInByDateUseCase = Objects.requireNonNull(getDailyRecoveryCheckInByDateUseCase);
		this.listDailyRecoveryCheckInsUseCase = Objects.requireNonNull(listDailyRecoveryCheckInsUseCase);
		this.listDailyRecoveryCheckInRevisionsUseCase = Objects.requireNonNull(listDailyRecoveryCheckInRevisionsUseCase);
		this.getRecoveryCheckInCalendarUseCase = Objects.requireNonNull(getRecoveryCheckInCalendarUseCase);
		this.getAthleteRecoveryHistoryUseCase = Objects.requireNonNull(getAthleteRecoveryHistoryUseCase);
		this.compareDailyRecoveryCheckInToBaselineUseCase =
				Objects.requireNonNull(compareDailyRecoveryCheckInToBaselineUseCase);
		this.compareRecoveryDateToBaselineUseCase =
				Objects.requireNonNull(compareRecoveryDateToBaselineUseCase);
	}

	@PostMapping("/api/v1/training/recovery-check-ins")
	@ResponseStatus(HttpStatus.CREATED)
	DailyRecoveryCheckInResponse create(
			@Valid @RequestBody CreateDailyRecoveryCheckInRequest request,
			Authentication authentication) {
		return DailyRecoveryCheckInResponse.from(createDailyRecoveryCheckInUseCase.execute(
				accountId(authentication),
				request.checkInDate(),
				request.sleepDurationMinutes(),
				request.sleepQuality(),
				request.fatigue(),
				request.muscleSoreness(),
				request.stress(),
				request.mood(),
				request.motivation(),
				toDiscomfortInputs(request.discomfortAreas()),
				request.notes()));
	}

	@PatchMapping("/api/v1/training/recovery-check-ins/{checkInId}")
	DailyRecoveryCheckInResponse update(
			@PathVariable UUID checkInId,
			@Valid @RequestBody UpdateDailyRecoveryCheckInRequest request,
			Authentication authentication) {
		return DailyRecoveryCheckInResponse.from(updateDailyRecoveryCheckInUseCase.execute(
				accountId(authentication),
				DailyRecoveryCheckInId.of(checkInId),
				toCommand(request)));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/{checkInId}")
	DailyRecoveryCheckInResponse getById(@PathVariable UUID checkInId, Authentication authentication) {
		return DailyRecoveryCheckInResponse.from(getDailyRecoveryCheckInUseCase.execute(
				accountId(authentication),
				DailyRecoveryCheckInId.of(checkInId)));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/by-date/{date}")
	DailyRecoveryCheckInResponse getByDate(@PathVariable LocalDate date, Authentication authentication) {
		return DailyRecoveryCheckInResponse.from(getDailyRecoveryCheckInByDateUseCase.execute(
				accountId(authentication),
				date));
	}

	@GetMapping("/api/v1/training/recovery-check-ins")
	DailyRecoveryCheckInListResponse list(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(required = false) RecoveryCheckInCompleteness completeness,
			@RequestParam(required = false) Integer minimumFatigue,
			@RequestParam(required = false) Integer minimumSoreness,
			@RequestParam(required = false) BodyArea bodyArea,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return DailyRecoveryCheckInListResponse.from(listDailyRecoveryCheckInsUseCase.execute(
				accountId(authentication),
				startDate,
				endDate,
				completeness,
				minimumFatigue,
				minimumSoreness,
				bodyArea,
				page,
				size));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/{checkInId}/revisions")
	DailyRecoveryCheckInRevisionListResponse revisions(
			@PathVariable UUID checkInId,
			Authentication authentication) {
		return new DailyRecoveryCheckInRevisionListResponse(listDailyRecoveryCheckInRevisionsUseCase.execute(
				accountId(authentication),
				DailyRecoveryCheckInId.of(checkInId)).stream()
				.map(DailyRecoveryCheckInRevisionResponse::from)
				.toList());
	}

	@GetMapping("/api/v1/training/recovery-check-ins/calendar")
	RecoveryCheckInCalendarResponse calendar(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			Authentication authentication) {
		return RecoveryCheckInCalendarResponse.from(getRecoveryCheckInCalendarUseCase.execute(
				accountId(authentication),
				startDate,
				endDate));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/{checkInId}/baseline-comparison")
	DailyRecoveryBaselineComparisonResponse baselineComparisonById(
			@PathVariable UUID checkInId,
			@RequestParam int baselineWindowDays,
			@RequestParam(defaultValue = "false") boolean includeTrainingLoad,
			Authentication authentication) {
		return DailyRecoveryBaselineComparisonResponse.from(compareDailyRecoveryCheckInToBaselineUseCase.execute(
				accountId(authentication),
				DailyRecoveryCheckInId.of(checkInId),
				baselineWindowDays,
				includeTrainingLoad));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/by-date/{date}/baseline-comparison")
	DailyRecoveryBaselineComparisonResponse baselineComparisonByDate(
			@PathVariable LocalDate date,
			@RequestParam int baselineWindowDays,
			@RequestParam(defaultValue = "false") boolean includeTrainingLoad,
			Authentication authentication) {
		return DailyRecoveryBaselineComparisonResponse.from(compareRecoveryDateToBaselineUseCase.execute(
				accountId(authentication),
				date,
				baselineWindowDays,
				includeTrainingLoad));
	}

	@GetMapping("/api/v1/training/recovery-check-ins/history")
	AthleteRecoveryHistoryResponse history(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam(defaultValue = "false") boolean includeTrainingLoad,
			Authentication authentication) {
		return AthleteRecoveryHistoryResponse.from(getAthleteRecoveryHistoryUseCase.execute(
				accountId(authentication),
				startDate,
				endDate,
				includeTrainingLoad));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return AccountId.of(principal.accountUuid());
	}

	private static List<BodyAreaDiscomfortObservation.Input> toDiscomfortInputs(
			List<BodyAreaDiscomfortRequest> requests) {
		if (requests == null) {
			return null;
		}
		return requests.stream()
				.map(request -> new BodyAreaDiscomfortObservation.Input(
						request.bodyArea(),
						request.side(),
						request.intensity(),
						request.notes()))
				.toList();
	}

	private static UpdateDailyRecoveryCheckInCommand toCommand(UpdateDailyRecoveryCheckInRequest request) {
		return new UpdateDailyRecoveryCheckInCommand(
				request.sleepDurationMinutes() == null ? null : request.sleepDurationMinutes().value(),
				request.sleepDurationMinutes() != null,
				request.sleepQuality() == null ? null : request.sleepQuality().value(),
				request.sleepQuality() != null,
				request.fatigue() == null ? null : request.fatigue().value(),
				request.fatigue() != null,
				request.muscleSoreness() == null ? null : request.muscleSoreness().value(),
				request.muscleSoreness() != null,
				request.stress() == null ? null : request.stress().value(),
				request.stress() != null,
				request.mood() == null ? null : request.mood().value(),
				request.mood() != null,
				request.motivation() == null ? null : request.motivation().value(),
				request.motivation() != null,
				request.discomfortAreas() == null
						? null
						: toDiscomfortInputs(request.discomfortAreas().value()),
				request.discomfortAreas() != null,
				request.notes() == null ? null : request.notes().value(),
				request.notes() != null,
				request.expectedVersion());
	}

}
