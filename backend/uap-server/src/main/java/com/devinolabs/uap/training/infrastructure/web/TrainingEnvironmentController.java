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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ArchiveTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.CreateTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.GetTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.ListTrainingEnvironmentsUseCase;
import com.devinolabs.uap.training.application.SetDefaultTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.UpdateTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

@RestController
@RequestMapping("/api/v1/training/environments")
class TrainingEnvironmentController {

	private final CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;
	private final ListTrainingEnvironmentsUseCase listTrainingEnvironmentsUseCase;
	private final GetTrainingEnvironmentUseCase getTrainingEnvironmentUseCase;
	private final UpdateTrainingEnvironmentUseCase updateTrainingEnvironmentUseCase;
	private final ArchiveTrainingEnvironmentUseCase archiveTrainingEnvironmentUseCase;
	private final SetDefaultTrainingEnvironmentUseCase setDefaultTrainingEnvironmentUseCase;

	TrainingEnvironmentController(
			CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase,
			ListTrainingEnvironmentsUseCase listTrainingEnvironmentsUseCase,
			GetTrainingEnvironmentUseCase getTrainingEnvironmentUseCase,
			UpdateTrainingEnvironmentUseCase updateTrainingEnvironmentUseCase,
			ArchiveTrainingEnvironmentUseCase archiveTrainingEnvironmentUseCase,
			SetDefaultTrainingEnvironmentUseCase setDefaultTrainingEnvironmentUseCase) {
		this.createTrainingEnvironmentUseCase = Objects.requireNonNull(createTrainingEnvironmentUseCase);
		this.listTrainingEnvironmentsUseCase = Objects.requireNonNull(listTrainingEnvironmentsUseCase);
		this.getTrainingEnvironmentUseCase = Objects.requireNonNull(getTrainingEnvironmentUseCase);
		this.updateTrainingEnvironmentUseCase = Objects.requireNonNull(updateTrainingEnvironmentUseCase);
		this.archiveTrainingEnvironmentUseCase = Objects.requireNonNull(archiveTrainingEnvironmentUseCase);
		this.setDefaultTrainingEnvironmentUseCase = Objects.requireNonNull(setDefaultTrainingEnvironmentUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TrainingEnvironmentResponse create(
			@Valid @RequestBody CreateTrainingEnvironmentRequest request,
			Authentication authentication) {
		return TrainingEnvironmentResponse.from(createTrainingEnvironmentUseCase.execute(
				accountId(authentication),
				request.name(),
				request.type(),
				request.availableEquipment(),
				request.description(),
				request.facilityNotes(),
				request.defaultEnvironment()));
	}

	@GetMapping
	TrainingEnvironmentPageResponse list(
			@RequestParam(required = false) TrainingEnvironmentType type,
			@RequestParam(required = false) List<EquipmentType> equipment,
			@RequestParam(required = false) Boolean activeOnly,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return TrainingEnvironmentPageResponse.from(listTrainingEnvironmentsUseCase.execute(
				accountId(authentication), type, equipment, activeOnly, page, size));
	}

	@GetMapping("/{environmentId}")
	TrainingEnvironmentResponse get(
			@PathVariable UUID environmentId,
			Authentication authentication) {
		return TrainingEnvironmentResponse.from(getTrainingEnvironmentUseCase.execute(
				accountId(authentication), TrainingEnvironmentId.of(environmentId)));
	}

	@PatchMapping("/{environmentId}")
	TrainingEnvironmentResponse update(
			@PathVariable UUID environmentId,
			@Valid @RequestBody UpdateTrainingEnvironmentRequest request,
			Authentication authentication) {
		return TrainingEnvironmentResponse.from(updateTrainingEnvironmentUseCase.execute(
				accountId(authentication),
				TrainingEnvironmentId.of(environmentId),
				UpdateTrainingEnvironmentRequestMapper.toCommand(request)));
	}

	@DeleteMapping("/{environmentId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID environmentId, Authentication authentication) {
		archiveTrainingEnvironmentUseCase.execute(
				accountId(authentication), TrainingEnvironmentId.of(environmentId));
	}

	@PostMapping("/{environmentId}/default")
	TrainingEnvironmentResponse setDefault(
			@PathVariable UUID environmentId,
			Authentication authentication) {
		return TrainingEnvironmentResponse.from(setDefaultTrainingEnvironmentUseCase.execute(
				accountId(authentication), TrainingEnvironmentId.of(environmentId)));
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}
}
