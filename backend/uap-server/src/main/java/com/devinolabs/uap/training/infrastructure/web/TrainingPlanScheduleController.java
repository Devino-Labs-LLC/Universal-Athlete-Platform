package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ActivateTrainingPlanScheduleCommand;
import com.devinolabs.uap.training.application.ActivateTrainingPlanScheduleUseCase;
import com.devinolabs.uap.training.application.CompleteTrainingPlanScheduleUseCase;
import com.devinolabs.uap.training.application.GenerateWorkoutOccurrencesUseCase;
import com.devinolabs.uap.training.application.PauseTrainingPlanScheduleUseCase;
import com.devinolabs.uap.training.application.ResumeTrainingPlanScheduleUseCase;
import com.devinolabs.uap.training.application.TrainingPlanScheduleActivationResult;
import com.devinolabs.uap.training.application.WorkoutOccurrenceGenerationResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/schedule")
class TrainingPlanScheduleController {

	private final ActivateTrainingPlanScheduleUseCase activateTrainingPlanScheduleUseCase;
	private final PauseTrainingPlanScheduleUseCase pauseTrainingPlanScheduleUseCase;
	private final ResumeTrainingPlanScheduleUseCase resumeTrainingPlanScheduleUseCase;
	private final CompleteTrainingPlanScheduleUseCase completeTrainingPlanScheduleUseCase;
	private final GenerateWorkoutOccurrencesUseCase generateWorkoutOccurrencesUseCase;

	TrainingPlanScheduleController(
			ActivateTrainingPlanScheduleUseCase activateTrainingPlanScheduleUseCase,
			PauseTrainingPlanScheduleUseCase pauseTrainingPlanScheduleUseCase,
			ResumeTrainingPlanScheduleUseCase resumeTrainingPlanScheduleUseCase,
			CompleteTrainingPlanScheduleUseCase completeTrainingPlanScheduleUseCase,
			GenerateWorkoutOccurrencesUseCase generateWorkoutOccurrencesUseCase) {
		this.activateTrainingPlanScheduleUseCase = Objects.requireNonNull(activateTrainingPlanScheduleUseCase);
		this.pauseTrainingPlanScheduleUseCase = Objects.requireNonNull(pauseTrainingPlanScheduleUseCase);
		this.resumeTrainingPlanScheduleUseCase = Objects.requireNonNull(resumeTrainingPlanScheduleUseCase);
		this.completeTrainingPlanScheduleUseCase = Objects.requireNonNull(completeTrainingPlanScheduleUseCase);
		this.generateWorkoutOccurrencesUseCase = Objects.requireNonNull(generateWorkoutOccurrencesUseCase);
	}

	@PostMapping("/activate")
	TrainingPlanScheduleActivationResponse activate(
			@PathVariable UUID planId,
			@Valid @RequestBody ActivateTrainingPlanScheduleRequest request,
			Authentication authentication) {
		TrainingPlanScheduleActivationResult result = activateTrainingPlanScheduleUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				new ActivateTrainingPlanScheduleCommand(
						request.scheduleStartDate(),
						request.scheduleEndDate(),
						request.timezone(),
						request.recurrenceMode(),
						request.generateThrough()));
		return new TrainingPlanScheduleActivationResponse(
				TrainingPlanController.toResponse(result.plan()),
				toGenerationResponse(result.generation()));
	}

	@PostMapping("/pause")
	TrainingPlanResponse pause(@PathVariable UUID planId, Authentication authentication) {
		return TrainingPlanController.toResponse(
				pauseTrainingPlanScheduleUseCase.execute(accountId(authentication), TrainingPlanId.of(planId)));
	}

	@PostMapping("/resume")
	TrainingPlanResponse resume(@PathVariable UUID planId, Authentication authentication) {
		return TrainingPlanController.toResponse(
				resumeTrainingPlanScheduleUseCase.execute(accountId(authentication), TrainingPlanId.of(planId)));
	}

	@PostMapping("/complete")
	TrainingPlanResponse complete(@PathVariable UUID planId, Authentication authentication) {
		return TrainingPlanController.toResponse(
				completeTrainingPlanScheduleUseCase.execute(accountId(authentication), TrainingPlanId.of(planId)));
	}

	@PostMapping("/generate")
	WorkoutOccurrenceGenerationResponse generate(
			@PathVariable UUID planId,
			@Valid @RequestBody GenerateWorkoutOccurrencesRequest request,
			Authentication authentication) {
		return toGenerationResponse(generateWorkoutOccurrencesUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				request.scheduledFrom(),
				request.scheduledTo()));
	}

	private static WorkoutOccurrenceGenerationResponse toGenerationResponse(
			WorkoutOccurrenceGenerationResult result) {
		if (result == null) {
			return null;
		}
		List<WorkoutOccurrenceResponse> created = result.createdOccurrences().stream()
				.map(WorkoutOccurrenceController::toResponse)
				.toList();
		return new WorkoutOccurrenceGenerationResponse(
				result.from(),
				result.to(),
				result.createdCount(),
				result.existingCount(),
				result.cancelledPlacementCount(),
				result.outOfScheduleCount(),
				result.scheduleGeneratedThrough(),
				created);
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}

record ActivateTrainingPlanScheduleRequest(
		@NotNull LocalDate scheduleStartDate,
		LocalDate scheduleEndDate,
		@NotNull String timezone,
		@NotNull TrainingPlanRecurrenceMode recurrenceMode,
		LocalDate generateThrough) {
}

record GenerateWorkoutOccurrencesRequest(
		@NotNull LocalDate scheduledFrom,
		@NotNull LocalDate scheduledTo) {
}

record TrainingPlanScheduleActivationResponse(
		TrainingPlanResponse plan,
		WorkoutOccurrenceGenerationResponse generation) {
}

record WorkoutOccurrenceGenerationResponse(
		LocalDate requestedFrom,
		LocalDate requestedTo,
		int createdCount,
		int existingCount,
		int cancelledPlacementCount,
		int outOfScheduleCount,
		LocalDate generatedThrough,
		List<WorkoutOccurrenceResponse> createdOccurrences) {
}
