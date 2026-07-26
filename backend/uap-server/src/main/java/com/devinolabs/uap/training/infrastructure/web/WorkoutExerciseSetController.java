package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.AddWorkoutExerciseSetCommand;
import com.devinolabs.uap.training.application.AddWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.CompleteWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.DeleteWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.GetWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.ListWorkoutExerciseSetsUseCase;
import com.devinolabs.uap.training.application.ReorderWorkoutExerciseSetsUseCase;
import com.devinolabs.uap.training.application.SkipWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.StartWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseSetCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseSetUseCase;
import com.devinolabs.uap.training.application.WorkoutExerciseSetResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}"
		+ "/exercises/{executionId}/sets")
class WorkoutExerciseSetController {

	private final ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;
	private final GetWorkoutExerciseSetUseCase getWorkoutExerciseSetUseCase;
	private final AddWorkoutExerciseSetUseCase addWorkoutExerciseSetUseCase;
	private final UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;
	private final StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;
	private final CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;
	private final SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase;
	private final DeleteWorkoutExerciseSetUseCase deleteWorkoutExerciseSetUseCase;
	private final ReorderWorkoutExerciseSetsUseCase reorderWorkoutExerciseSetsUseCase;

	WorkoutExerciseSetController(
			ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase,
			GetWorkoutExerciseSetUseCase getWorkoutExerciseSetUseCase,
			AddWorkoutExerciseSetUseCase addWorkoutExerciseSetUseCase,
			UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase,
			StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase,
			CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase,
			SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase,
			DeleteWorkoutExerciseSetUseCase deleteWorkoutExerciseSetUseCase,
			ReorderWorkoutExerciseSetsUseCase reorderWorkoutExerciseSetsUseCase) {
		this.listWorkoutExerciseSetsUseCase = Objects.requireNonNull(listWorkoutExerciseSetsUseCase);
		this.getWorkoutExerciseSetUseCase = Objects.requireNonNull(getWorkoutExerciseSetUseCase);
		this.addWorkoutExerciseSetUseCase = Objects.requireNonNull(addWorkoutExerciseSetUseCase);
		this.updateWorkoutExerciseSetUseCase = Objects.requireNonNull(updateWorkoutExerciseSetUseCase);
		this.startWorkoutExerciseSetUseCase = Objects.requireNonNull(startWorkoutExerciseSetUseCase);
		this.completeWorkoutExerciseSetUseCase = Objects.requireNonNull(completeWorkoutExerciseSetUseCase);
		this.skipWorkoutExerciseSetUseCase = Objects.requireNonNull(skipWorkoutExerciseSetUseCase);
		this.deleteWorkoutExerciseSetUseCase = Objects.requireNonNull(deleteWorkoutExerciseSetUseCase);
		this.reorderWorkoutExerciseSetsUseCase = Objects.requireNonNull(reorderWorkoutExerciseSetsUseCase);
	}

	@GetMapping
	List<WorkoutExerciseSetResponse> list(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return listWorkoutExerciseSetsUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						WorkoutOccurrenceId.of(occurrenceId),
						WorkoutExerciseExecutionId.of(executionId))
				.stream()
				.map(WorkoutExerciseSetController::toResponse)
				.toList();
	}

	@GetMapping("/{setId}")
	WorkoutExerciseSetResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			Authentication authentication) {
		return toResponse(getWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId)));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutExerciseSetResponse add(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@Valid @RequestBody(required = false) AddWorkoutExerciseSetRequest request,
			Authentication authentication) {
		AddWorkoutExerciseSetRequest body = request == null
				? new AddWorkoutExerciseSetRequest(null, null, null, null, null, null, null, null, null, null, null)
				: request;
		AddWorkoutExerciseSetCommand command = new AddWorkoutExerciseSetCommand(
				body.copyFromSetId() == null ? null : WorkoutExerciseSetId.of(body.copyFromSetId()),
				body.setType(),
				body.prescribedMinimumReps(),
				body.prescribedMaximumReps(),
				body.prescribedWeight(),
				body.prescribedWeightUnit(),
				body.prescribedDurationSeconds(),
				body.prescribedDistance(),
				body.prescribedDistanceUnit(),
				body.prescribedTargetRpe(),
				body.prescribedRestSeconds());
		return toResponse(addWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				command));
	}

	@PatchMapping("/{setId}")
	WorkoutExerciseSetResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			@Valid @RequestBody UpdateWorkoutExerciseSetRequest request,
			Authentication authentication) {
		UpdateWorkoutExerciseSetCommand command = new UpdateWorkoutExerciseSetCommand(
				request.setType() == null ? null : request.setType().value(),
				request.setType() != null,
				request.actualReps() == null ? null : request.actualReps().value(),
				request.actualReps() != null,
				request.actualWeight() == null ? null : request.actualWeight().value(),
				request.actualWeight() != null,
				request.actualWeightUnit() == null ? null : request.actualWeightUnit().value(),
				request.actualWeightUnit() != null,
				request.actualDurationSeconds() == null ? null : request.actualDurationSeconds().value(),
				request.actualDurationSeconds() != null,
				request.actualDistance() == null ? null : request.actualDistance().value(),
				request.actualDistance() != null,
				request.actualDistanceUnit() == null ? null : request.actualDistanceUnit().value(),
				request.actualDistanceUnit() != null,
				request.actualRestSeconds() == null ? null : request.actualRestSeconds().value(),
				request.actualRestSeconds() != null,
				request.actualRpe() == null ? null : request.actualRpe().value(),
				request.actualRpe() != null,
				request.athleteNotes() == null ? null : request.athleteNotes().value(),
				request.athleteNotes() != null);
		return toResponse(updateWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId),
				command));
	}

	@PostMapping("/{setId}/start")
	WorkoutExerciseSetResponse start(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			Authentication authentication) {
		return toResponse(startWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId)));
	}

	@PostMapping("/{setId}/complete")
	WorkoutExerciseSetResponse complete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			Authentication authentication) {
		return toResponse(completeWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId)));
	}

	@PostMapping("/{setId}/skip")
	WorkoutExerciseSetResponse skip(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			Authentication authentication) {
		return toResponse(skipWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId)));
	}

	@DeleteMapping("/{setId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@PathVariable UUID setId,
			Authentication authentication) {
		deleteWorkoutExerciseSetUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				WorkoutExerciseSetId.of(setId));
	}

	@PostMapping("/reorder")
	List<WorkoutExerciseSetResponse> reorder(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@Valid @RequestBody ReorderWorkoutExerciseSetsRequest request,
			Authentication authentication) {
		return reorderWorkoutExerciseSetsUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						WorkoutOccurrenceId.of(occurrenceId),
						WorkoutExerciseExecutionId.of(executionId),
						request.setIds())
				.stream()
				.map(WorkoutExerciseSetController::toResponse)
				.toList();
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = requirePrincipal(authentication);
		return AccountId.of(principal.accountUuid());
	}

	private static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal;
	}

	static WorkoutExerciseSetResponse toResponse(WorkoutExerciseSetResult result) {
		return new WorkoutExerciseSetResponse(
				result.id().value(),
				result.workoutExerciseExecutionId().value(),
				result.setNumber(),
				result.displayOrder(),
				result.setType(),
				result.prescribedMinimumReps(),
				result.prescribedMaximumReps(),
				result.prescribedWeight(),
				result.prescribedWeightUnit(),
				result.prescribedDurationSeconds(),
				result.prescribedDistance(),
				result.prescribedDistanceUnit(),
				result.prescribedTargetRpe(),
				result.prescribedRestSeconds(),
				result.actualReps(),
				result.actualWeight(),
				result.actualWeightUnit(),
				result.actualDurationSeconds(),
				result.actualDistance(),
				result.actualDistanceUnit(),
				result.actualRestSeconds(),
				result.actualRpe(),
				result.status(),
				result.startedAt(),
				result.completedAt(),
				result.athleteNotes(),
				result.createdAt(),
				result.updatedAt());
	}

}
