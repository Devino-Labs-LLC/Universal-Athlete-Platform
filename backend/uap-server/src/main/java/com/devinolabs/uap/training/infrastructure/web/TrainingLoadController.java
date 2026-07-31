package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
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
import com.devinolabs.uap.training.application.GetAthleteTrainingLoadHistoryUseCase;
import com.devinolabs.uap.training.application.GetWorkoutOccurrenceLoadSummaryUseCase;
import com.devinolabs.uap.training.application.GetWorkoutSessionEffortUseCase;
import com.devinolabs.uap.training.application.ListWorkoutSessionEffortRevisionsUseCase;
import com.devinolabs.uap.training.application.RebuildAthleteTrainingLoadUseCase;
import com.devinolabs.uap.training.application.RecomputeWorkoutOccurrenceLoadUseCase;
import com.devinolabs.uap.training.application.SubmitWorkoutSessionEffortUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutSessionEffortUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
class TrainingLoadController {

	private final SubmitWorkoutSessionEffortUseCase submitWorkoutSessionEffortUseCase;
	private final UpdateWorkoutSessionEffortUseCase updateWorkoutSessionEffortUseCase;
	private final GetWorkoutSessionEffortUseCase getWorkoutSessionEffortUseCase;
	private final ListWorkoutSessionEffortRevisionsUseCase listWorkoutSessionEffortRevisionsUseCase;
	private final GetWorkoutOccurrenceLoadSummaryUseCase getWorkoutOccurrenceLoadSummaryUseCase;
	private final RecomputeWorkoutOccurrenceLoadUseCase recomputeWorkoutOccurrenceLoadUseCase;
	private final RebuildAthleteTrainingLoadUseCase rebuildAthleteTrainingLoadUseCase;
	private final GetAthleteTrainingLoadHistoryUseCase getAthleteTrainingLoadHistoryUseCase;

	TrainingLoadController(
			SubmitWorkoutSessionEffortUseCase submitWorkoutSessionEffortUseCase,
			UpdateWorkoutSessionEffortUseCase updateWorkoutSessionEffortUseCase,
			GetWorkoutSessionEffortUseCase getWorkoutSessionEffortUseCase,
			ListWorkoutSessionEffortRevisionsUseCase listWorkoutSessionEffortRevisionsUseCase,
			GetWorkoutOccurrenceLoadSummaryUseCase getWorkoutOccurrenceLoadSummaryUseCase,
			RecomputeWorkoutOccurrenceLoadUseCase recomputeWorkoutOccurrenceLoadUseCase,
			RebuildAthleteTrainingLoadUseCase rebuildAthleteTrainingLoadUseCase,
			GetAthleteTrainingLoadHistoryUseCase getAthleteTrainingLoadHistoryUseCase) {
		this.submitWorkoutSessionEffortUseCase = Objects.requireNonNull(submitWorkoutSessionEffortUseCase);
		this.updateWorkoutSessionEffortUseCase = Objects.requireNonNull(updateWorkoutSessionEffortUseCase);
		this.getWorkoutSessionEffortUseCase = Objects.requireNonNull(getWorkoutSessionEffortUseCase);
		this.listWorkoutSessionEffortRevisionsUseCase = Objects.requireNonNull(listWorkoutSessionEffortRevisionsUseCase);
		this.getWorkoutOccurrenceLoadSummaryUseCase = Objects.requireNonNull(getWorkoutOccurrenceLoadSummaryUseCase);
		this.recomputeWorkoutOccurrenceLoadUseCase = Objects.requireNonNull(recomputeWorkoutOccurrenceLoadUseCase);
		this.rebuildAthleteTrainingLoadUseCase = Objects.requireNonNull(rebuildAthleteTrainingLoadUseCase);
		this.getAthleteTrainingLoadHistoryUseCase = Objects.requireNonNull(getAthleteTrainingLoadHistoryUseCase);
	}

	@PostMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/session-effort")
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutSessionEffortResponse submitSessionEffort(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@Valid @RequestBody SubmitWorkoutSessionEffortRequest request,
			Authentication authentication) {
		return WorkoutSessionEffortResponse.from(submitWorkoutSessionEffortUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				request.sessionRpe(),
				request.sessionDurationMinutes(),
				request.perceivedNotes()));
	}

	@PatchMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/session-effort")
	WorkoutSessionEffortResponse updateSessionEffort(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@Valid @RequestBody UpdateWorkoutSessionEffortRequest request,
			Authentication authentication) {
		return WorkoutSessionEffortResponse.from(updateWorkoutSessionEffortUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				request.sessionRpe(),
				request.sessionDurationMinutes(),
				request.perceivedNotes()));
	}

	@GetMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/session-effort")
	WorkoutSessionEffortResponse getSessionEffort(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return WorkoutSessionEffortResponse.from(getWorkoutSessionEffortUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@GetMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/session-effort/revisions")
	WorkoutSessionEffortRevisionListResponse listSessionEffortRevisions(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return new WorkoutSessionEffortRevisionListResponse(listWorkoutSessionEffortRevisionsUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)).stream()
				.map(WorkoutSessionEffortRevisionResponse::from)
				.toList());
	}

	@GetMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/training-load")
	WorkoutOccurrenceLoadSummaryResponse getOccurrenceLoad(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return WorkoutOccurrenceLoadSummaryResponse.from(getWorkoutOccurrenceLoadSummaryUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/training-load/recompute")
	WorkoutOccurrenceLoadSummaryResponse recomputeOccurrenceLoad(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return WorkoutOccurrenceLoadSummaryResponse.from(recomputeWorkoutOccurrenceLoadUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/api/v1/training/training-load/rebuild")
	RebuildTrainingLoadResponse rebuildTrainingLoad(Authentication authentication) {
		return RebuildTrainingLoadResponse.from(rebuildAthleteTrainingLoadUseCase.execute(
				accountId(authentication)));
	}

	@GetMapping("/api/v1/training/training-load/history")
	TrainingLoadHistoryResponse history(
			@RequestParam LocalDate startDate,
			@RequestParam LocalDate endDate,
			@RequestParam TrainingLoadGranularity granularity,
			@RequestParam(required = false) UUID trainingPlanId,
			@RequestParam(required = false) UUID workoutDayId,
			@RequestParam(required = false) ExerciseDefinitionCategory category,
			@RequestParam(required = false) MovementPattern movementPattern,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return TrainingLoadHistoryResponse.from(getAthleteTrainingLoadHistoryUseCase.execute(
				accountId(authentication),
				startDate,
				endDate,
				granularity,
				trainingPlanId,
				workoutDayId,
				category,
				movementPattern,
				page,
				size));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return AccountId.of(principal.accountUuid());
	}

}
