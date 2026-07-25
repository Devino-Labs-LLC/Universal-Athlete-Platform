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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ChangeWorkoutExerciseStatusUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.DeleteWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.GetWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.ListWorkoutExercisesUseCase;
import com.devinolabs.uap.training.application.ReorderWorkoutExercisesUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.WorkoutExerciseResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/exercises")
class WorkoutExerciseController {

	private final CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;
	private final ListWorkoutExercisesUseCase listWorkoutExercisesUseCase;
	private final GetWorkoutExerciseUseCase getWorkoutExerciseUseCase;
	private final UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase;
	private final ReorderWorkoutExercisesUseCase reorderWorkoutExercisesUseCase;
	private final ChangeWorkoutExerciseStatusUseCase changeWorkoutExerciseStatusUseCase;
	private final DeleteWorkoutExerciseUseCase deleteWorkoutExerciseUseCase;

	WorkoutExerciseController(
			CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase,
			ListWorkoutExercisesUseCase listWorkoutExercisesUseCase,
			GetWorkoutExerciseUseCase getWorkoutExerciseUseCase,
			UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase,
			ReorderWorkoutExercisesUseCase reorderWorkoutExercisesUseCase,
			ChangeWorkoutExerciseStatusUseCase changeWorkoutExerciseStatusUseCase,
			DeleteWorkoutExerciseUseCase deleteWorkoutExerciseUseCase) {
		this.createWorkoutExerciseUseCase = Objects.requireNonNull(createWorkoutExerciseUseCase);
		this.listWorkoutExercisesUseCase = Objects.requireNonNull(listWorkoutExercisesUseCase);
		this.getWorkoutExerciseUseCase = Objects.requireNonNull(getWorkoutExerciseUseCase);
		this.updateWorkoutExerciseUseCase = Objects.requireNonNull(updateWorkoutExerciseUseCase);
		this.reorderWorkoutExercisesUseCase = Objects.requireNonNull(reorderWorkoutExercisesUseCase);
		this.changeWorkoutExerciseStatusUseCase = Objects.requireNonNull(changeWorkoutExerciseStatusUseCase);
		this.deleteWorkoutExerciseUseCase = Objects.requireNonNull(deleteWorkoutExerciseUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutExerciseResponse create(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@Valid @RequestBody CreateWorkoutExerciseRequest request,
			Authentication authentication) {
		return toResponse(createWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				request.exerciseName(),
				request.category(),
				request.type(),
				request.sets(),
				request.minimumReps(),
				request.maximumReps(),
				request.targetWeight(),
				request.weightUnit(),
				request.targetDurationSeconds(),
				request.targetDistance(),
				request.distanceUnit(),
				request.targetRestSeconds(),
				request.targetRpe(),
				request.tempo(),
				request.coachingNotes(),
				request.displayOrder()));
	}

	@GetMapping
	List<WorkoutExerciseResponse> list(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			Authentication authentication) {
		return listWorkoutExercisesUseCase
				.execute(accountId(authentication), TrainingPlanId.of(planId), WorkoutDayId.of(dayId))
				.stream()
				.map(WorkoutExerciseController::toResponse)
				.toList();
	}

	@GetMapping("/{exerciseId}")
	WorkoutExerciseResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		return toResponse(getWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId)));
	}

	@PatchMapping("/{exerciseId}")
	WorkoutExerciseResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			@Valid @RequestBody UpdateWorkoutExerciseRequest request,
			Authentication authentication) {
		UpdateWorkoutExerciseCommand command = new UpdateWorkoutExerciseCommand(
				request.exerciseName() == null ? null : request.exerciseName().value(),
				request.exerciseName() != null,
				request.category() == null ? null : request.category().value(),
				request.category() != null,
				request.type() == null ? null : request.type().value(),
				request.type() != null,
				request.sets() == null ? null : request.sets().value(),
				request.sets() != null,
				request.minimumReps() == null ? null : request.minimumReps().value(),
				request.minimumReps() != null,
				request.maximumReps() == null ? null : request.maximumReps().value(),
				request.maximumReps() != null,
				request.targetWeight() == null ? null : request.targetWeight().value(),
				request.targetWeight() != null,
				request.weightUnit() == null ? null : request.weightUnit().value(),
				request.weightUnit() != null,
				request.targetDurationSeconds() == null ? null : request.targetDurationSeconds().value(),
				request.targetDurationSeconds() != null,
				request.targetDistance() == null ? null : request.targetDistance().value(),
				request.targetDistance() != null,
				request.distanceUnit() == null ? null : request.distanceUnit().value(),
				request.distanceUnit() != null,
				request.targetRestSeconds() == null ? null : request.targetRestSeconds().value(),
				request.targetRestSeconds() != null,
				request.targetRpe() == null ? null : request.targetRpe().value(),
				request.targetRpe() != null,
				request.tempo() == null ? null : request.tempo().value(),
				request.tempo() != null,
				request.coachingNotes() == null ? null : request.coachingNotes().value(),
				request.coachingNotes() != null,
				request.displayOrder() == null ? null : request.displayOrder().value(),
				request.displayOrder() != null);
		return toResponse(updateWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId),
				command));
	}

	@PatchMapping("/{exerciseId}/status")
	WorkoutExerciseResponse changeStatus(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			@Valid @RequestBody WorkoutExerciseStatusRequest request,
			Authentication authentication) {
		return toResponse(changeWorkoutExerciseStatusUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId),
				request.action()));
	}

	@PutMapping("/order")
	List<WorkoutExerciseResponse> reorder(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@Valid @RequestBody ReorderWorkoutExercisesRequest request,
			Authentication authentication) {
		return reorderWorkoutExercisesUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						request.exerciseIds())
				.stream()
				.map(WorkoutExerciseController::toResponse)
				.toList();
	}

	@DeleteMapping("/{exerciseId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		deleteWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId));
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

	private static WorkoutExerciseResponse toResponse(WorkoutExerciseResult result) {
		return new WorkoutExerciseResponse(
				result.id().value(),
				result.displayOrder(),
				result.exerciseName(),
				result.category(),
				result.type(),
				result.sets(),
				result.minimumReps(),
				result.maximumReps(),
				result.targetWeight(),
				result.weightUnit(),
				result.targetDurationSeconds(),
				result.targetDistance(),
				result.distanceUnit(),
				result.targetRestSeconds(),
				result.targetRpe(),
				result.tempo(),
				result.coachingNotes(),
				result.status(),
				result.createdAt(),
				result.updatedAt());
	}

}
