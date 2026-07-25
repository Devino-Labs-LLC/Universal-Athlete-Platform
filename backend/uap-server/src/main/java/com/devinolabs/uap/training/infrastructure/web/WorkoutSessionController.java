package com.devinolabs.uap.training.infrastructure.web;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CompleteWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.GetWorkoutSessionUseCase;
import com.devinolabs.uap.training.application.SkipWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.StartWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutSessionCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutSessionUseCase;
import com.devinolabs.uap.training.application.WorkoutSessionResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/exercises/{exerciseId}")
class WorkoutSessionController {

	private final StartWorkoutExerciseUseCase startWorkoutExerciseUseCase;
	private final GetWorkoutSessionUseCase getWorkoutSessionUseCase;
	private final UpdateWorkoutSessionUseCase updateWorkoutSessionUseCase;
	private final CompleteWorkoutExerciseUseCase completeWorkoutExerciseUseCase;
	private final SkipWorkoutExerciseUseCase skipWorkoutExerciseUseCase;

	WorkoutSessionController(
			StartWorkoutExerciseUseCase startWorkoutExerciseUseCase,
			GetWorkoutSessionUseCase getWorkoutSessionUseCase,
			UpdateWorkoutSessionUseCase updateWorkoutSessionUseCase,
			CompleteWorkoutExerciseUseCase completeWorkoutExerciseUseCase,
			SkipWorkoutExerciseUseCase skipWorkoutExerciseUseCase) {
		this.startWorkoutExerciseUseCase = Objects.requireNonNull(startWorkoutExerciseUseCase);
		this.getWorkoutSessionUseCase = Objects.requireNonNull(getWorkoutSessionUseCase);
		this.updateWorkoutSessionUseCase = Objects.requireNonNull(updateWorkoutSessionUseCase);
		this.completeWorkoutExerciseUseCase = Objects.requireNonNull(completeWorkoutExerciseUseCase);
		this.skipWorkoutExerciseUseCase = Objects.requireNonNull(skipWorkoutExerciseUseCase);
	}

	@PostMapping("/start")
	@ResponseStatus(HttpStatus.OK)
	WorkoutSessionResponse start(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		return toResponse(startWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId)));
	}

	@GetMapping("/session")
	WorkoutSessionResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		return toResponse(getWorkoutSessionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId)));
	}

	@PatchMapping("/session")
	WorkoutSessionResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			@Valid @RequestBody UpdateWorkoutSessionRequest request,
			Authentication authentication) {
		UpdateWorkoutSessionCommand command = new UpdateWorkoutSessionCommand(
				request.actualSets() == null ? null : request.actualSets().value(),
				request.actualSets() != null,
				request.actualReps() == null ? null : request.actualReps().value(),
				request.actualReps() != null,
				request.actualWeight() == null ? null : request.actualWeight().value(),
				request.actualWeight() != null,
				request.weightUnit() == null ? null : request.weightUnit().value(),
				request.weightUnit() != null,
				request.actualDurationSeconds() == null ? null : request.actualDurationSeconds().value(),
				request.actualDurationSeconds() != null,
				request.actualDistance() == null ? null : request.actualDistance().value(),
				request.actualDistance() != null,
				request.distanceUnit() == null ? null : request.distanceUnit().value(),
				request.distanceUnit() != null,
				request.actualRestSeconds() == null ? null : request.actualRestSeconds().value(),
				request.actualRestSeconds() != null,
				request.actualRpe() == null ? null : request.actualRpe().value(),
				request.actualRpe() != null,
				request.athleteNotes() == null ? null : request.athleteNotes().value(),
				request.athleteNotes() != null);
		return toResponse(updateWorkoutSessionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId),
				command));
	}

	@PostMapping("/complete")
	WorkoutSessionResponse complete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		return toResponse(completeWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId)));
	}

	@PostMapping("/skip")
	WorkoutSessionResponse skip(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID exerciseId,
			Authentication authentication) {
		return toResponse(skipWorkoutExerciseUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutExerciseId.of(exerciseId)));
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

	private static WorkoutSessionResponse toResponse(WorkoutSessionResult result) {
		return new WorkoutSessionResponse(
				result.id().value(),
				result.status(),
				result.actualSets(),
				result.actualReps(),
				result.actualWeight(),
				result.weightUnit(),
				result.actualDurationSeconds(),
				result.actualDistance(),
				result.distanceUnit(),
				result.actualRestSeconds(),
				result.actualRpe(),
				result.completedAt(),
				result.athleteNotes(),
				result.createdAt(),
				result.updatedAt());
	}

}
