package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.AthleteTrainingAssignmentUseCase;

@RestController
@RequestMapping("/api/v1/athletes/me/training/assignments")
class AthleteTrainingAssignmentController {

	private final AthleteTrainingAssignmentUseCase athleteTrainingAssignmentUseCase;

	AthleteTrainingAssignmentController(AthleteTrainingAssignmentUseCase athleteTrainingAssignmentUseCase) {
		this.athleteTrainingAssignmentUseCase = Objects.requireNonNull(athleteTrainingAssignmentUseCase);
	}

	@GetMapping
	List<TrainingAssignmentResponse> list(Authentication authentication) {
		return TrainingAssignmentResponse.fromList(
				athleteTrainingAssignmentUseCase.listMine(accountId(authentication)));
	}

	@PostMapping("/{assignmentId}/decline")
	TrainingAssignmentResponse decline(
			@PathVariable UUID assignmentId,
			@RequestBody(required = false) TrainingAssignmentResponseRequest request,
			Authentication authentication) {
		return TrainingAssignmentResponse.from(athleteTrainingAssignmentUseCase.decline(
				accountId(authentication),
				assignmentId,
				request == null ? null : request.note()));
	}

	@PostMapping("/{assignmentId}/unable")
	TrainingAssignmentResponse unable(
			@PathVariable UUID assignmentId,
			@RequestBody(required = false) TrainingAssignmentResponseRequest request,
			Authentication authentication) {
		return TrainingAssignmentResponse.from(athleteTrainingAssignmentUseCase.markUnable(
				accountId(authentication),
				assignmentId,
				request == null ? null : request.note()));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
