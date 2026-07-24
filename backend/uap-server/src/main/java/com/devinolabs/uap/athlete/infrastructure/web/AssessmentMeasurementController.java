package com.devinolabs.uap.athlete.infrastructure.web;

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

import com.devinolabs.uap.athlete.application.AssessmentMeasurementResult;
import com.devinolabs.uap.athlete.application.AttachMeasurementToAssessmentUseCase;
import com.devinolabs.uap.athlete.application.DetachMeasurementFromAssessmentUseCase;
import com.devinolabs.uap.athlete.application.ListAssessmentMeasurementsUseCase;
import com.devinolabs.uap.athlete.application.ReorderAssessmentMeasurementsUseCase;
import com.devinolabs.uap.athlete.application.UpdateAssessmentMeasurementCommand;
import com.devinolabs.uap.athlete.application.UpdateAssessmentMeasurementUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes/me/assessments/{assessmentId}/measurements")
class AssessmentMeasurementController {

	private final AttachMeasurementToAssessmentUseCase attachMeasurementToAssessmentUseCase;
	private final ListAssessmentMeasurementsUseCase listAssessmentMeasurementsUseCase;
	private final UpdateAssessmentMeasurementUseCase updateAssessmentMeasurementUseCase;
	private final ReorderAssessmentMeasurementsUseCase reorderAssessmentMeasurementsUseCase;
	private final DetachMeasurementFromAssessmentUseCase detachMeasurementFromAssessmentUseCase;

	AssessmentMeasurementController(
			AttachMeasurementToAssessmentUseCase attachMeasurementToAssessmentUseCase,
			ListAssessmentMeasurementsUseCase listAssessmentMeasurementsUseCase,
			UpdateAssessmentMeasurementUseCase updateAssessmentMeasurementUseCase,
			ReorderAssessmentMeasurementsUseCase reorderAssessmentMeasurementsUseCase,
			DetachMeasurementFromAssessmentUseCase detachMeasurementFromAssessmentUseCase) {
		this.attachMeasurementToAssessmentUseCase = Objects.requireNonNull(attachMeasurementToAssessmentUseCase);
		this.listAssessmentMeasurementsUseCase = Objects.requireNonNull(listAssessmentMeasurementsUseCase);
		this.updateAssessmentMeasurementUseCase = Objects.requireNonNull(updateAssessmentMeasurementUseCase);
		this.reorderAssessmentMeasurementsUseCase = Objects.requireNonNull(reorderAssessmentMeasurementsUseCase);
		this.detachMeasurementFromAssessmentUseCase = Objects.requireNonNull(detachMeasurementFromAssessmentUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AssessmentMeasurementResponse attach(
			@PathVariable UUID assessmentId,
			@Valid @RequestBody AttachAssessmentMeasurementRequest request,
			Authentication authentication) {
		return toResponse(attachMeasurementToAssessmentUseCase.execute(
				accountId(authentication),
				AssessmentId.of(assessmentId),
				AthleteMeasurementId.of(request.measurementId()),
				request.displayOrder(),
				request.label(),
				request.notes()));
	}

	@GetMapping
	List<AssessmentMeasurementResponse> list(@PathVariable UUID assessmentId, Authentication authentication) {
		return listAssessmentMeasurementsUseCase.execute(accountId(authentication), AssessmentId.of(assessmentId))
				.stream()
				.map(AssessmentMeasurementController::toResponse)
				.toList();
	}

	@PatchMapping("/{attachmentId}")
	AssessmentMeasurementResponse update(
			@PathVariable UUID assessmentId,
			@PathVariable UUID attachmentId,
			@Valid @RequestBody UpdateAssessmentMeasurementRequest request,
			Authentication authentication) {
		UpdateAssessmentMeasurementCommand command = new UpdateAssessmentMeasurementCommand(
				request.displayOrder() == null ? null : request.displayOrder().value(),
				request.displayOrder() != null,
				request.label() == null ? null : request.label().value(),
				request.label() != null,
				request.notes() == null ? null : request.notes().value(),
				request.notes() != null);
		return toResponse(updateAssessmentMeasurementUseCase.execute(
				accountId(authentication),
				AssessmentId.of(assessmentId),
				AssessmentMeasurementId.of(attachmentId),
				command));
	}

	@PutMapping("/order")
	List<AssessmentMeasurementResponse> reorder(
			@PathVariable UUID assessmentId,
			@Valid @RequestBody ReorderAssessmentMeasurementsRequest request,
			Authentication authentication) {
		return reorderAssessmentMeasurementsUseCase
				.execute(accountId(authentication), AssessmentId.of(assessmentId), request.attachmentIds())
				.stream()
				.map(AssessmentMeasurementController::toResponse)
				.toList();
	}

	@DeleteMapping("/{attachmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void detach(
			@PathVariable UUID assessmentId,
			@PathVariable UUID attachmentId,
			Authentication authentication) {
		detachMeasurementFromAssessmentUseCase.execute(
				accountId(authentication),
				AssessmentId.of(assessmentId),
				AssessmentMeasurementId.of(attachmentId));
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

	private static AssessmentMeasurementResponse toResponse(AssessmentMeasurementResult result) {
		return new AssessmentMeasurementResponse(
				result.id().value(),
				result.measurementId().value(),
				result.displayOrder(),
				result.label(),
				result.notes(),
				result.snapshotted(),
				result.measurementType(),
				result.customMeasurementName(),
				result.value(),
				result.unit(),
				result.customUnit(),
				result.source(),
				result.measuredAt(),
				result.athleteSportId(),
				result.athleteGoalId(),
				result.snapshottedAt(),
				result.createdAt(),
				result.updatedAt());
	}

}
