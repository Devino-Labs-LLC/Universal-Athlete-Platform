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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ApplyWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.CancelWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.GenerateWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.GetWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.ListWorkoutAdaptationProposalsUseCase;
import com.devinolabs.uap.training.application.RegenerateWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutAdaptationProposalItemUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@RestController
class WorkoutAdaptationProposalController {

	private final GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase;
	private final GetWorkoutAdaptationProposalUseCase getWorkoutAdaptationProposalUseCase;
	private final ListWorkoutAdaptationProposalsUseCase listWorkoutAdaptationProposalsUseCase;
	private final UpdateWorkoutAdaptationProposalItemUseCase updateWorkoutAdaptationProposalItemUseCase;
	private final CancelWorkoutAdaptationProposalUseCase cancelWorkoutAdaptationProposalUseCase;
	private final RegenerateWorkoutAdaptationProposalUseCase regenerateWorkoutAdaptationProposalUseCase;
	private final ApplyWorkoutAdaptationProposalUseCase applyWorkoutAdaptationProposalUseCase;

	WorkoutAdaptationProposalController(
			GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase,
			GetWorkoutAdaptationProposalUseCase getWorkoutAdaptationProposalUseCase,
			ListWorkoutAdaptationProposalsUseCase listWorkoutAdaptationProposalsUseCase,
			UpdateWorkoutAdaptationProposalItemUseCase updateWorkoutAdaptationProposalItemUseCase,
			CancelWorkoutAdaptationProposalUseCase cancelWorkoutAdaptationProposalUseCase,
			RegenerateWorkoutAdaptationProposalUseCase regenerateWorkoutAdaptationProposalUseCase,
			ApplyWorkoutAdaptationProposalUseCase applyWorkoutAdaptationProposalUseCase) {
		this.generateWorkoutAdaptationProposalUseCase = Objects.requireNonNull(generateWorkoutAdaptationProposalUseCase);
		this.getWorkoutAdaptationProposalUseCase = Objects.requireNonNull(getWorkoutAdaptationProposalUseCase);
		this.listWorkoutAdaptationProposalsUseCase = Objects.requireNonNull(listWorkoutAdaptationProposalsUseCase);
		this.updateWorkoutAdaptationProposalItemUseCase = Objects.requireNonNull(updateWorkoutAdaptationProposalItemUseCase);
		this.cancelWorkoutAdaptationProposalUseCase = Objects.requireNonNull(cancelWorkoutAdaptationProposalUseCase);
		this.regenerateWorkoutAdaptationProposalUseCase = Objects.requireNonNull(regenerateWorkoutAdaptationProposalUseCase);
		this.applyWorkoutAdaptationProposalUseCase = Objects.requireNonNull(applyWorkoutAdaptationProposalUseCase);
	}

	@PostMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/adaptation-proposals")
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutAdaptationProposalResponse generate(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@RequestBody(required = false) GenerateWorkoutAdaptationProposalRequest request,
			Authentication authentication) {
		GenerateWorkoutAdaptationProposalRequest resolved = request == null
				? new GenerateWorkoutAdaptationProposalRequest(null, null, null)
				: request;
		return WorkoutAdaptationProposalResponse.from(generateWorkoutAdaptationProposalUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				resolved.suggestionLimit(),
				resolved.includeAlternatives(),
				resolved.expirationMinutes()));
	}

	@GetMapping("/api/v1/training/adaptation-proposals/{proposalId}")
	WorkoutAdaptationProposalResponse get(
			@PathVariable UUID proposalId,
			Authentication authentication) {
		return WorkoutAdaptationProposalResponse.from(getWorkoutAdaptationProposalUseCase.execute(
				accountId(authentication),
				WorkoutAdaptationProposalId.of(proposalId)));
	}

	@GetMapping("/api/v1/training/adaptation-proposals")
	List<WorkoutAdaptationProposalSummaryResponse> list(
			@RequestParam(required = false) UUID occurrenceId,
			@RequestParam(required = false) WorkoutAdaptationProposalStatus status,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return listWorkoutAdaptationProposalsUseCase.execute(
				accountId(authentication),
				occurrenceId == null ? null : WorkoutOccurrenceId.of(occurrenceId),
				status,
				page,
				size).stream()
				.map(WorkoutAdaptationProposalSummaryResponse::from)
				.toList();
	}

	@PatchMapping("/api/v1/training/adaptation-proposals/{proposalId}/items/{itemId}")
	WorkoutAdaptationProposalResponse updateItem(
			@PathVariable UUID proposalId,
			@PathVariable UUID itemId,
			@Valid @RequestBody UpdateWorkoutAdaptationProposalItemRequest request,
			Authentication authentication) {
		return WorkoutAdaptationProposalResponse.from(updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId(authentication),
				WorkoutAdaptationProposalId.of(proposalId),
				WorkoutAdaptationProposalItemId.of(itemId),
				request.decision(),
				request.targetExerciseDefinitionId() == null
						? null
						: ExerciseDefinitionId.of(request.targetExerciseDefinitionId()),
				request.substitutionRelationshipId() == null
						? null
						: ExerciseSubstitutionRelationshipId.of(request.substitutionRelationshipId()),
				request.athleteNotes()));
	}

	@PostMapping("/api/v1/training/adaptation-proposals/{proposalId}/cancel")
	WorkoutAdaptationProposalResponse cancel(
			@PathVariable UUID proposalId,
			Authentication authentication) {
		return WorkoutAdaptationProposalResponse.from(cancelWorkoutAdaptationProposalUseCase.execute(
				accountId(authentication),
				WorkoutAdaptationProposalId.of(proposalId)));
	}

	@PostMapping("/api/v1/training/adaptation-proposals/{proposalId}/regenerate")
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutAdaptationProposalResponse regenerate(
			@PathVariable UUID proposalId,
			@RequestBody(required = false) GenerateWorkoutAdaptationProposalRequest request,
			Authentication authentication) {
		GenerateWorkoutAdaptationProposalRequest resolved = request == null
				? new GenerateWorkoutAdaptationProposalRequest(null, null, null)
				: request;
		return WorkoutAdaptationProposalResponse.from(regenerateWorkoutAdaptationProposalUseCase.execute(
				accountId(authentication),
				WorkoutAdaptationProposalId.of(proposalId),
				resolved.suggestionLimit(),
				resolved.includeAlternatives(),
				resolved.expirationMinutes()));
	}

	@PostMapping("/api/v1/training/plans/{planId}/days/{dayId}/occurrences/{occurrenceId}/adaptation-proposals/{proposalId}/apply")
	WorkoutAdaptationApplicationResponse apply(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@PathVariable UUID occurrenceId,
			@PathVariable UUID proposalId,
			@Valid @RequestBody ApplyWorkoutAdaptationProposalRequest request,
			Authentication authentication) {
		return WorkoutAdaptationApplicationResponse.from(applyWorkoutAdaptationProposalUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				WorkoutOccurrenceId.of(occurrenceId),
				WorkoutAdaptationProposalId.of(proposalId),
				request.expectedProposalVersion()));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return AccountId.of(principal.accountUuid());
	}

}
