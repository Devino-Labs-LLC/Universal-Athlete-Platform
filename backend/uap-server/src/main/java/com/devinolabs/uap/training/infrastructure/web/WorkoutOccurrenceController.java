package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CancelWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.CompleteWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.DeleteWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.GetWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.ListWorkoutOccurrencesUseCase;
import com.devinolabs.uap.training.application.RescheduleWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.SetWorkoutOccurrenceTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.ClearWorkoutOccurrenceTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.SkipWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.StartWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutOccurrenceCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionResult;
import com.devinolabs.uap.training.application.WorkoutOccurrenceDetailResult;
import com.devinolabs.uap.training.application.WorkoutOccurrenceResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences")
class WorkoutOccurrenceController {

	private final CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;
	private final ListWorkoutOccurrencesUseCase listWorkoutOccurrencesUseCase;
	private final GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;
	private final UpdateWorkoutOccurrenceUseCase updateWorkoutOccurrenceUseCase;
	private final StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase;
	private final CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;
	private final SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase;
	private final CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase;
	private final DeleteWorkoutOccurrenceUseCase deleteWorkoutOccurrenceUseCase;
	private final RescheduleWorkoutOccurrenceUseCase rescheduleWorkoutOccurrenceUseCase;
	private final SetWorkoutOccurrenceTrainingEnvironmentUseCase setWorkoutOccurrenceTrainingEnvironmentUseCase;
	private final ClearWorkoutOccurrenceTrainingEnvironmentUseCase clearWorkoutOccurrenceTrainingEnvironmentUseCase;

	WorkoutOccurrenceController(
			CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase,
			ListWorkoutOccurrencesUseCase listWorkoutOccurrencesUseCase,
			GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase,
			UpdateWorkoutOccurrenceUseCase updateWorkoutOccurrenceUseCase,
			StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase,
			CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase,
			SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase,
			CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase,
			DeleteWorkoutOccurrenceUseCase deleteWorkoutOccurrenceUseCase,
			RescheduleWorkoutOccurrenceUseCase rescheduleWorkoutOccurrenceUseCase,
			SetWorkoutOccurrenceTrainingEnvironmentUseCase setWorkoutOccurrenceTrainingEnvironmentUseCase,
			ClearWorkoutOccurrenceTrainingEnvironmentUseCase clearWorkoutOccurrenceTrainingEnvironmentUseCase) {
		this.createWorkoutOccurrenceUseCase = Objects.requireNonNull(createWorkoutOccurrenceUseCase);
		this.listWorkoutOccurrencesUseCase = Objects.requireNonNull(listWorkoutOccurrencesUseCase);
		this.getWorkoutOccurrenceUseCase = Objects.requireNonNull(getWorkoutOccurrenceUseCase);
		this.updateWorkoutOccurrenceUseCase = Objects.requireNonNull(updateWorkoutOccurrenceUseCase);
		this.startWorkoutOccurrenceUseCase = Objects.requireNonNull(startWorkoutOccurrenceUseCase);
		this.completeWorkoutOccurrenceUseCase = Objects.requireNonNull(completeWorkoutOccurrenceUseCase);
		this.skipWorkoutOccurrenceUseCase = Objects.requireNonNull(skipWorkoutOccurrenceUseCase);
		this.cancelWorkoutOccurrenceUseCase = Objects.requireNonNull(cancelWorkoutOccurrenceUseCase);
		this.deleteWorkoutOccurrenceUseCase = Objects.requireNonNull(deleteWorkoutOccurrenceUseCase);
		this.rescheduleWorkoutOccurrenceUseCase = Objects.requireNonNull(rescheduleWorkoutOccurrenceUseCase);
		this.setWorkoutOccurrenceTrainingEnvironmentUseCase = Objects.requireNonNull(setWorkoutOccurrenceTrainingEnvironmentUseCase);
		this.clearWorkoutOccurrenceTrainingEnvironmentUseCase = Objects.requireNonNull(clearWorkoutOccurrenceTrainingEnvironmentUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutOccurrenceDetailResponse create(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@Valid @RequestBody CreateWorkoutOccurrenceRequest request,
			Authentication authentication) {
		return toDetailResponse(createWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				request.scheduledDate(),
				request.plannedStartTime(),
				request.athleteNotes()));
	}

	@GetMapping
	List<WorkoutOccurrenceResponse> list(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@RequestParam(required = false) WorkoutOccurrenceStatus status,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledFrom,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate scheduledTo,
			Authentication authentication) {
		return listWorkoutOccurrencesUseCase
				.execute(
						accountId(authentication),
						TrainingPlanId.of(planId),
						WorkoutDayId.of(dayId),
						status,
						scheduledFrom,
						scheduledTo)
				.stream()
				.map(WorkoutOccurrenceController::toResponse)
				.toList();
	}

	@GetMapping("/{occurrenceId}")
	WorkoutOccurrenceDetailResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return toDetailResponse(getWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PatchMapping("/{occurrenceId}")
	WorkoutOccurrenceDetailResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@Valid @RequestBody UpdateWorkoutOccurrenceRequest request,
			Authentication authentication) {
		UpdateWorkoutOccurrenceCommand command = new UpdateWorkoutOccurrenceCommand(
				request.scheduledDate() == null ? null : request.scheduledDate().value(),
				request.scheduledDate() != null,
				request.plannedStartTime() == null ? null : request.plannedStartTime().value(),
				request.plannedStartTime() != null,
				request.athleteNotes() == null ? null : request.athleteNotes().value(),
				request.athleteNotes() != null);
		return toDetailResponse(updateWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				command));
	}

	@PostMapping("/{occurrenceId}/reschedule")
	WorkoutOccurrenceDetailResponse reschedule(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@Valid @RequestBody RescheduleWorkoutOccurrenceRequest request,
			Authentication authentication) {
		return toDetailResponse(rescheduleWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				request.scheduledDate(),
				request.plannedStartTime()));
	}

	@PostMapping("/{occurrenceId}/start")
	WorkoutOccurrenceDetailResponse start(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return toDetailResponse(startWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/{occurrenceId}/complete")
	WorkoutOccurrenceDetailResponse complete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return toDetailResponse(completeWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/{occurrenceId}/skip")
	WorkoutOccurrenceDetailResponse skip(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return toDetailResponse(skipWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@PostMapping("/{occurrenceId}/cancel")
	WorkoutOccurrenceDetailResponse cancel(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return toDetailResponse(cancelWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
	}

	@DeleteMapping("/{occurrenceId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		deleteWorkoutOccurrenceUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId));
	}


	@PutMapping("/{occurrenceId}/environment")
	WorkoutOccurrenceResponse setEnvironment(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@Valid @RequestBody SetWorkoutOccurrenceEnvironmentRequest request,
			Authentication authentication) {
		return WorkoutOccurrenceResponse.from(setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				TrainingEnvironmentId.of(request.trainingEnvironmentId())));
	}

	@DeleteMapping("/{occurrenceId}/environment")
	WorkoutOccurrenceResponse clearEnvironment(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			Authentication authentication) {
		return WorkoutOccurrenceResponse.from(clearWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId)));
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

	static WorkoutOccurrenceResponse toResponse(WorkoutOccurrenceResult result) {
		return WorkoutOccurrenceResponse.from(result);
	}

	static WorkoutOccurrenceDetailResponse toDetailResponse(WorkoutOccurrenceDetailResult detail) {
		WorkoutOccurrenceResult occurrence = detail.occurrence();
		return new WorkoutOccurrenceDetailResponse(
				occurrence.id().value(),
				occurrence.workoutDayId().value(),
				occurrence.scheduledDate(),
				occurrence.plannedStartTime(),
				occurrence.startedAt(),
				occurrence.completedAt(),
				occurrence.status(),
				occurrence.athleteNotes(),
				occurrence.origin(),
				occurrence.originalScheduledDate(),
				occurrence.manuallyRescheduled(),
				WorkoutOccurrenceEnvironmentContextResponse.from(occurrence.environment()),
				occurrence.createdAt(),
				occurrence.updatedAt(),
				detail.executions().stream().map(WorkoutOccurrenceController::toExecutionResponse).toList());
	}

	static WorkoutExerciseExecutionResponse toExecutionResponse(WorkoutExerciseExecutionResult result) {
		return new WorkoutExerciseExecutionResponse(
				result.id().value(),
				result.sourceWorkoutExerciseId().value(),
				result.prescribedExerciseDefinitionId().value(),
				result.prescribedExerciseName(),
				result.performedExerciseDefinitionId().value(),
				result.performedExerciseName(),
				result.exercisePerformanceKey().value(),
				result.substituted(),
				result.substitutionReason(),
				result.substitutionNotes(),
				result.substitutedAt(),
				result.displayOrder(),
				result.exerciseName(),
				result.category(),
				result.type(),
				result.prescribedSets(),
				result.prescribedMinimumReps(),
				result.prescribedMaximumReps(),
				result.prescribedTargetWeight(),
				result.prescribedWeightUnit(),
				result.prescribedTargetDurationSeconds(),
				result.prescribedTargetDistance(),
				result.prescribedDistanceUnit(),
				result.prescribedTargetRestSeconds(),
				result.prescribedTargetRpe(),
				result.prescribedTempo(),
				result.prescribedCoachingNotes(),
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
				result.startedAt(),
				result.completedAt(),
				result.athleteNotes(),
				result.createdAt(),
				result.updatedAt(),
				result.setCounts().setCount(),
				result.setCounts().notStartedSetCount(),
				result.setCounts().inProgressSetCount(),
				result.setCounts().completedSetCount(),
				result.setCounts().skippedSetCount());
	}

}
