package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
import com.devinolabs.uap.training.application.AssignCoachTrainingUseCase;
import com.devinolabs.uap.training.application.UpdateCoachTrainingAssignmentUseCase;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/athletes/{athleteId}/training/assignments")
class CoachTrainingAssignmentController {

	private final AssignCoachTrainingUseCase assignCoachTrainingUseCase;
	private final UpdateCoachTrainingAssignmentUseCase updateCoachTrainingAssignmentUseCase;

	CoachTrainingAssignmentController(
			AssignCoachTrainingUseCase assignCoachTrainingUseCase,
			UpdateCoachTrainingAssignmentUseCase updateCoachTrainingAssignmentUseCase) {
		this.assignCoachTrainingUseCase = Objects.requireNonNull(assignCoachTrainingUseCase);
		this.updateCoachTrainingAssignmentUseCase = Objects.requireNonNull(updateCoachTrainingAssignmentUseCase);
	}

	@GetMapping
	List<TrainingAssignmentResponse> list(
			@PathVariable UUID teamId,
			@PathVariable UUID athleteId,
			Authentication authentication) {
		return TrainingAssignmentResponse.fromList(
				assignCoachTrainingUseCase.list(accountId(authentication), teamId, athleteId));
	}

	@GetMapping("/{assignmentId}")
	TrainingAssignmentResponse get(
			@PathVariable UUID teamId,
			@PathVariable UUID athleteId,
			@PathVariable UUID assignmentId,
			Authentication authentication) {
		return TrainingAssignmentResponse.from(
				assignCoachTrainingUseCase.get(accountId(authentication), teamId, athleteId, assignmentId));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TrainingAssignmentResponse create(
			@PathVariable UUID teamId,
			@PathVariable UUID athleteId,
			@RequestBody CreateTrainingAssignmentRequest request,
			Authentication authentication) {
		return TrainingAssignmentResponse.from(assignCoachTrainingUseCase.execute(
				accountId(authentication),
				teamId,
				athleteId,
				request.title(),
				request.description(),
				request.scheduledDate(),
				request.idempotencyKey()));
	}

	@PatchMapping("/{assignmentId}")
	TrainingAssignmentResponse update(
			@PathVariable UUID teamId,
			@PathVariable UUID athleteId,
			@PathVariable UUID assignmentId,
			@RequestBody UpdateTrainingAssignmentRequest request,
			Authentication authentication) {
		return TrainingAssignmentResponse.from(updateCoachTrainingAssignmentUseCase.execute(
				accountId(authentication),
				teamId,
				athleteId,
				assignmentId,
				request.expectedVersion(),
				request.title(),
				request.description(),
				request.scheduledDate()));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
