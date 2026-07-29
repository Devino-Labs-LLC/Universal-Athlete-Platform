package com.devinolabs.uap.training.infrastructure.web;

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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CompleteWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.GetWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.ListWorkoutExerciseExecutionsUseCase;
import com.devinolabs.uap.training.application.ListWorkoutExerciseSubstitutionHistoryUseCase;
import com.devinolabs.uap.training.application.RevertWorkoutExerciseExecutionSubstitutionUseCase;
import com.devinolabs.uap.training.application.SkipWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.StartWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.SubstituteWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseExecutionCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutExerciseExecutionUseCase;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/exercises")
class WorkoutExerciseExecutionController {

	private final ListWorkoutExerciseExecutionsUseCase listWorkoutExerciseExecutionsUseCase;
	private final GetWorkoutExerciseExecutionUseCase getWorkoutExerciseExecutionUseCase;
	private final StartWorkoutExerciseExecutionUseCase startWorkoutExerciseExecutionUseCase;
	private final UpdateWorkoutExerciseExecutionUseCase updateWorkoutExerciseExecutionUseCase;
	private final CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;
	private final SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase;
	private final SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;
	private final RevertWorkoutExerciseExecutionSubstitutionUseCase
			revertWorkoutExerciseExecutionSubstitutionUseCase;
	private final ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	WorkoutExerciseExecutionController(
			ListWorkoutExerciseExecutionsUseCase listWorkoutExerciseExecutionsUseCase,
			GetWorkoutExerciseExecutionUseCase getWorkoutExerciseExecutionUseCase,
			StartWorkoutExerciseExecutionUseCase startWorkoutExerciseExecutionUseCase,
			UpdateWorkoutExerciseExecutionUseCase updateWorkoutExerciseExecutionUseCase,
			CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase,
			SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase,
			SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase,
			RevertWorkoutExerciseExecutionSubstitutionUseCase revertWorkoutExerciseExecutionSubstitutionUseCase,
			ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase) {
		this.listWorkoutExerciseExecutionsUseCase = Objects.requireNonNull(listWorkoutExerciseExecutionsUseCase);
		this.getWorkoutExerciseExecutionUseCase = Objects.requireNonNull(getWorkoutExerciseExecutionUseCase);
		this.startWorkoutExerciseExecutionUseCase = Objects.requireNonNull(startWorkoutExerciseExecutionUseCase);
		this.updateWorkoutExerciseExecutionUseCase = Objects.requireNonNull(updateWorkoutExerciseExecutionUseCase);
		this.completeWorkoutExerciseExecutionUseCase = Objects.requireNonNull(completeWorkoutExerciseExecutionUseCase);
		this.skipWorkoutExerciseExecutionUseCase = Objects.requireNonNull(skipWorkoutExerciseExecutionUseCase);
		this.substituteWorkoutExerciseExecutionUseCase = Objects.requireNonNull(
				substituteWorkoutExerciseExecutionUseCase);
		this.revertWorkoutExerciseExecutionSubstitutionUseCase = Objects.requireNonNull(
				revertWorkoutExerciseExecutionSubstitutionUseCase);
		this.listWorkoutExerciseSubstitutionHistoryUseCase = Objects.requireNonNull(
				listWorkoutExerciseSubstitutionHistoryUseCase);
	}

	@GetMapping
	List<WorkoutExerciseExecutionResponse> list(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return listWorkoutExerciseExecutionsUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						WorkoutOccurrenceId.of(occurrenceId))
				.stream()
				.map(WorkoutOccurrenceController::toExecutionResponse)
				.toList();
	}

	@GetMapping("/{executionId}")
	WorkoutExerciseExecutionResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(getWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId)));
	}

	@PostMapping("/{executionId}/start")
	@ResponseStatus(HttpStatus.OK)
	WorkoutExerciseExecutionResponse start(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(startWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId)));
	}

	@PatchMapping("/{executionId}")
	WorkoutExerciseExecutionResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@Valid @RequestBody UpdateWorkoutExerciseExecutionRequest request,
			Authentication authentication) {
		UpdateWorkoutExerciseExecutionCommand command = new UpdateWorkoutExerciseExecutionCommand(
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
		return WorkoutOccurrenceController.toExecutionResponse(updateWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				command));
	}

	@PostMapping("/{executionId}/complete")
	WorkoutExerciseExecutionResponse complete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(completeWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId)));
	}

	@PostMapping("/{executionId}/skip")
	WorkoutExerciseExecutionResponse skip(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(skipWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId)));
	}

	@PostMapping("/{executionId}/substitute")
	WorkoutExerciseExecutionResponse substitute(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@Valid @RequestBody SubstituteWorkoutExerciseExecutionRequest request,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(substituteWorkoutExerciseExecutionUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutExerciseExecutionId.of(executionId),
				ExerciseDefinitionId.of(request.exerciseDefinitionId()),
				request.reason(),
				request.notes()));
	}

	@PostMapping("/{executionId}/substitute/revert")
	WorkoutExerciseExecutionResponse revertSubstitution(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			@Valid @RequestBody(required = false) RevertWorkoutExerciseExecutionSubstitutionRequest request,
			Authentication authentication) {
		return WorkoutOccurrenceController.toExecutionResponse(
				revertWorkoutExerciseExecutionSubstitutionUseCase.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						WorkoutOccurrenceId.of(occurrenceId),
						WorkoutExerciseExecutionId.of(executionId),
						request == null ? null : request.notes()));
	}

	@GetMapping("/{executionId}/substitutions")
	List<WorkoutExerciseSubstitutionResponse> substitutions(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID executionId,
			Authentication authentication) {
		return listWorkoutExerciseSubstitutionHistoryUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						WorkoutOccurrenceId.of(occurrenceId),
						WorkoutExerciseExecutionId.of(executionId))
				.stream()
				.map(WorkoutExerciseSubstitutionResponse::from)
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

}
