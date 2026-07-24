package com.devinolabs.uap.athlete.infrastructure.web;

import java.time.Instant;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.athlete.application.AssessmentResult;
import com.devinolabs.uap.athlete.application.ChangeAssessmentStatusUseCase;
import com.devinolabs.uap.athlete.application.CreateAssessmentUseCase;
import com.devinolabs.uap.athlete.application.DeleteAssessmentUseCase;
import com.devinolabs.uap.athlete.application.GetAssessmentUseCase;
import com.devinolabs.uap.athlete.application.ListAssessmentsUseCase;
import com.devinolabs.uap.athlete.application.UpdateAssessmentCommand;
import com.devinolabs.uap.athlete.application.UpdateAssessmentUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes/me/assessments")
class AssessmentController {

	private final CreateAssessmentUseCase createAssessmentUseCase;
	private final ListAssessmentsUseCase listAssessmentsUseCase;
	private final GetAssessmentUseCase getAssessmentUseCase;
	private final UpdateAssessmentUseCase updateAssessmentUseCase;
	private final ChangeAssessmentStatusUseCase changeAssessmentStatusUseCase;
	private final DeleteAssessmentUseCase deleteAssessmentUseCase;

	AssessmentController(
			CreateAssessmentUseCase createAssessmentUseCase,
			ListAssessmentsUseCase listAssessmentsUseCase,
			GetAssessmentUseCase getAssessmentUseCase,
			UpdateAssessmentUseCase updateAssessmentUseCase,
			ChangeAssessmentStatusUseCase changeAssessmentStatusUseCase,
			DeleteAssessmentUseCase deleteAssessmentUseCase) {
		this.createAssessmentUseCase = Objects.requireNonNull(createAssessmentUseCase);
		this.listAssessmentsUseCase = Objects.requireNonNull(listAssessmentsUseCase);
		this.getAssessmentUseCase = Objects.requireNonNull(getAssessmentUseCase);
		this.updateAssessmentUseCase = Objects.requireNonNull(updateAssessmentUseCase);
		this.changeAssessmentStatusUseCase = Objects.requireNonNull(changeAssessmentStatusUseCase);
		this.deleteAssessmentUseCase = Objects.requireNonNull(deleteAssessmentUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AssessmentResponse create(@Valid @RequestBody CreateAssessmentRequest request, Authentication authentication) {
		return toResponse(createAssessmentUseCase.execute(
				accountId(authentication),
				request.type(),
				request.customTypeName(),
				request.title(),
				request.description(),
				request.scheduledAt(),
				request.notes(),
				request.athleteSportId(),
				request.athleteGoalId()));
	}

	@GetMapping
	List<AssessmentResponse> list(
			@RequestParam(required = false) AssessmentStatus status,
			@RequestParam(required = false) AssessmentType assessmentType,
			@RequestParam(required = false) Instant scheduledFrom,
			@RequestParam(required = false) Instant scheduledTo,
			Authentication authentication) {
		return listAssessmentsUseCase
				.execute(accountId(authentication), status, assessmentType, scheduledFrom, scheduledTo)
				.stream()
				.map(AssessmentController::toResponse)
				.toList();
	}

	@GetMapping("/{assessmentId}")
	AssessmentResponse get(@PathVariable UUID assessmentId, Authentication authentication) {
		return toResponse(getAssessmentUseCase.execute(accountId(authentication), AssessmentId.of(assessmentId)));
	}

	@PatchMapping("/{assessmentId}")
	AssessmentResponse update(
			@PathVariable UUID assessmentId,
			@Valid @RequestBody UpdateAssessmentRequest request,
			Authentication authentication) {
		UpdateAssessmentCommand command = new UpdateAssessmentCommand(
				request.title() == null ? null : request.title().value(),
				request.title() != null,
				request.description() == null ? null : request.description().value(),
				request.description() != null,
				request.notes() == null ? null : request.notes().value(),
				request.notes() != null,
				request.scheduledAt() == null ? null : request.scheduledAt().value(),
				request.scheduledAt() != null,
				request.athleteSportId() == null ? null : request.athleteSportId().value(),
				request.athleteSportId() != null,
				request.athleteGoalId() == null ? null : request.athleteGoalId().value(),
				request.athleteGoalId() != null);
		return toResponse(updateAssessmentUseCase.execute(
				accountId(authentication),
				AssessmentId.of(assessmentId),
				command));
	}

	@PatchMapping("/{assessmentId}/status")
	AssessmentResponse changeStatus(
			@PathVariable UUID assessmentId,
			@Valid @RequestBody AssessmentStatusRequest request,
			Authentication authentication) {
		return toResponse(changeAssessmentStatusUseCase.execute(
				accountId(authentication),
				AssessmentId.of(assessmentId),
				request.action()));
	}

	@DeleteMapping("/{assessmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID assessmentId, Authentication authentication) {
		deleteAssessmentUseCase.execute(accountId(authentication), AssessmentId.of(assessmentId));
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

	private static AssessmentResponse toResponse(AssessmentResult result) {
		return new AssessmentResponse(
				result.id().value(),
				result.type(),
				result.customTypeName(),
				result.title(),
				result.description(),
				result.status(),
				result.scheduledAt(),
				result.startedAt(),
				result.completedAt(),
				result.notes(),
				result.athleteSportId() == null ? null : result.athleteSportId().value(),
				result.athleteGoalId() == null ? null : result.athleteGoalId().value(),
				result.createdAt(),
				result.updatedAt());
	}

}
