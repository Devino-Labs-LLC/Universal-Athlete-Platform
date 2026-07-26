package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.AthleteCalendarEntryResult;
import com.devinolabs.uap.training.application.AthleteTrainingTodayResult;
import com.devinolabs.uap.training.application.GetAthleteTrainingCalendarUseCase;
import com.devinolabs.uap.training.application.GetAthleteTrainingTodayUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@RestController
@RequestMapping("/api/v1/training/calendar")
class TrainingCalendarController {

	private final GetAthleteTrainingCalendarUseCase getAthleteTrainingCalendarUseCase;
	private final GetAthleteTrainingTodayUseCase getAthleteTrainingTodayUseCase;

	TrainingCalendarController(
			GetAthleteTrainingCalendarUseCase getAthleteTrainingCalendarUseCase,
			GetAthleteTrainingTodayUseCase getAthleteTrainingTodayUseCase) {
		this.getAthleteTrainingCalendarUseCase = Objects.requireNonNull(getAthleteTrainingCalendarUseCase);
		this.getAthleteTrainingTodayUseCase = Objects.requireNonNull(getAthleteTrainingTodayUseCase);
	}

	@GetMapping
	List<AthleteCalendarEntryResponse> calendar(
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledFrom,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledTo,
			@RequestParam(required = false) WorkoutOccurrenceStatus status,
			@RequestParam(required = false) UUID trainingPlanId,
			Authentication authentication) {
		return getAthleteTrainingCalendarUseCase.execute(
						accountId(authentication),
						scheduledFrom,
						scheduledTo,
						status,
						trainingPlanId == null ? null : TrainingPlanId.of(trainingPlanId))
				.stream()
				.map(TrainingCalendarController::toResponse)
				.toList();
	}

	@GetMapping("/today")
	AthleteTrainingTodayResponse today(
			@RequestParam(required = false) String timezone,
			Authentication authentication) {
		AthleteTrainingTodayResult result = getAthleteTrainingTodayUseCase.execute(
				accountId(authentication), timezone);
		return new AthleteTrainingTodayResponse(
				result.date(),
				result.timezone(),
				result.entries().stream().map(TrainingCalendarController::toResponse).toList());
	}

	private static AthleteCalendarEntryResponse toResponse(AthleteCalendarEntryResult entry) {
		return new AthleteCalendarEntryResponse(
				entry.occurrenceId().value(),
				entry.trainingPlanId().value(),
				entry.trainingPlanName(),
				entry.workoutDayId().value(),
				entry.workoutDayName(),
				entry.scheduledDate(),
				entry.plannedStartTime(),
				entry.status(),
				entry.origin(),
				entry.manuallyRescheduled(),
				entry.originalScheduledDate(),
				entry.startedAt(),
				entry.completedAt(),
				entry.athleteNotes(),
				entry.exerciseCount(),
				entry.notStartedExerciseCount(),
				entry.inProgressExerciseCount(),
				entry.completedExerciseCount(),
				entry.skippedExerciseCount());
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}

record AthleteCalendarEntryResponse(
		UUID occurrenceId,
		UUID trainingPlanId,
		String trainingPlanName,
		UUID workoutDayId,
		String workoutDayName,
		LocalDate scheduledDate,
		LocalTime plannedStartTime,
		WorkoutOccurrenceStatus status,
		WorkoutOccurrenceOrigin origin,
		boolean manuallyRescheduled,
		LocalDate originalScheduledDate,
		Instant startedAt,
		Instant completedAt,
		String athleteNotes,
		int exerciseCount,
		int notStartedExerciseCount,
		int inProgressExerciseCount,
		int completedExerciseCount,
		int skippedExerciseCount) {
}

record AthleteTrainingTodayResponse(
		LocalDate date,
		String timezone,
		List<AthleteCalendarEntryResponse> entries) {
}
