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

import com.devinolabs.uap.athlete.application.AthleteMeasurementResult;
import com.devinolabs.uap.athlete.application.DeleteAthleteMeasurementUseCase;
import com.devinolabs.uap.athlete.application.GetCurrentAthleteMeasurementUseCase;
import com.devinolabs.uap.athlete.application.ListCurrentAthleteMeasurementsUseCase;
import com.devinolabs.uap.athlete.application.RecordAthleteMeasurementUseCase;
import com.devinolabs.uap.athlete.application.UpdateAthleteMeasurementCommand;
import com.devinolabs.uap.athlete.application.UpdateAthleteMeasurementUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes/me/measurements")
class AthleteMeasurementController {

	private final RecordAthleteMeasurementUseCase recordAthleteMeasurementUseCase;
	private final ListCurrentAthleteMeasurementsUseCase listCurrentAthleteMeasurementsUseCase;
	private final GetCurrentAthleteMeasurementUseCase getCurrentAthleteMeasurementUseCase;
	private final UpdateAthleteMeasurementUseCase updateAthleteMeasurementUseCase;
	private final DeleteAthleteMeasurementUseCase deleteAthleteMeasurementUseCase;

	AthleteMeasurementController(
			RecordAthleteMeasurementUseCase recordAthleteMeasurementUseCase,
			ListCurrentAthleteMeasurementsUseCase listCurrentAthleteMeasurementsUseCase,
			GetCurrentAthleteMeasurementUseCase getCurrentAthleteMeasurementUseCase,
			UpdateAthleteMeasurementUseCase updateAthleteMeasurementUseCase,
			DeleteAthleteMeasurementUseCase deleteAthleteMeasurementUseCase) {
		this.recordAthleteMeasurementUseCase = Objects.requireNonNull(recordAthleteMeasurementUseCase);
		this.listCurrentAthleteMeasurementsUseCase = Objects.requireNonNull(listCurrentAthleteMeasurementsUseCase);
		this.getCurrentAthleteMeasurementUseCase = Objects.requireNonNull(getCurrentAthleteMeasurementUseCase);
		this.updateAthleteMeasurementUseCase = Objects.requireNonNull(updateAthleteMeasurementUseCase);
		this.deleteAthleteMeasurementUseCase = Objects.requireNonNull(deleteAthleteMeasurementUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AthleteMeasurementResponse record(
			@Valid @RequestBody RecordAthleteMeasurementRequest request,
			Authentication authentication) {
		return toResponse(recordAthleteMeasurementUseCase.execute(
				accountId(authentication),
				request.measurementType(),
				request.customMeasurementName(),
				request.value(),
				request.unit(),
				request.customUnit(),
				request.source(),
				request.notes(),
				request.measuredAt(),
				request.athleteSportId(),
				request.athleteGoalId()));
	}

	@GetMapping
	List<AthleteMeasurementResponse> list(
			@RequestParam(required = false) MeasurementType measurementType,
			@RequestParam(required = false) MeasurementSource source,
			@RequestParam(required = false) UUID athleteSportId,
			@RequestParam(required = false) UUID athleteGoalId,
			@RequestParam(required = false) Instant measuredFrom,
			@RequestParam(required = false) Instant measuredTo,
			Authentication authentication) {
		return listCurrentAthleteMeasurementsUseCase.execute(
						accountId(authentication),
						measurementType,
						source,
						athleteSportId,
						athleteGoalId,
						measuredFrom,
						measuredTo)
				.stream()
				.map(AthleteMeasurementController::toResponse)
				.toList();
	}

	@GetMapping("/{measurementId}")
	AthleteMeasurementResponse get(@PathVariable UUID measurementId, Authentication authentication) {
		return toResponse(getCurrentAthleteMeasurementUseCase.execute(
				accountId(authentication),
				AthleteMeasurementId.of(measurementId)));
	}

	@PatchMapping("/{measurementId}")
	AthleteMeasurementResponse update(
			@PathVariable UUID measurementId,
			@Valid @RequestBody UpdateAthleteMeasurementRequest request,
			Authentication authentication) {
		UpdateAthleteMeasurementCommand command = new UpdateAthleteMeasurementCommand(
				request.value() == null ? null : request.value().value(),
				request.value() != null,
				request.unit() == null ? null : request.unit().value(),
				request.unit() != null,
				request.customUnit() == null ? null : request.customUnit().value(),
				request.customUnit() != null,
				request.notes() == null ? null : request.notes().value(),
				request.notes() != null,
				request.measuredAt() == null ? null : request.measuredAt().value(),
				request.measuredAt() != null,
				request.athleteSportId() == null ? null : request.athleteSportId().value(),
				request.athleteSportId() != null,
				request.athleteGoalId() == null ? null : request.athleteGoalId().value(),
				request.athleteGoalId() != null);
		return toResponse(updateAthleteMeasurementUseCase.execute(
				accountId(authentication),
				AthleteMeasurementId.of(measurementId),
				command));
	}

	@DeleteMapping("/{measurementId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID measurementId, Authentication authentication) {
		deleteAthleteMeasurementUseCase.execute(accountId(authentication), AthleteMeasurementId.of(measurementId));
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

	private static AthleteMeasurementResponse toResponse(AthleteMeasurementResult result) {
		return new AthleteMeasurementResponse(
				result.id().value(),
				result.measurementType(),
				result.customMeasurementName(),
				result.value(),
				result.unit(),
				result.customUnit(),
				result.source(),
				result.notes(),
				result.measuredAt(),
				result.athleteSportId() == null ? null : result.athleteSportId().value(),
				result.athleteGoalId() == null ? null : result.athleteGoalId().value(),
				result.createdAt(),
				result.updatedAt());
	}

}
