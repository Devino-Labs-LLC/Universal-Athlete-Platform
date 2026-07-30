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
import com.devinolabs.uap.training.application.ChangeTrainingPlanStatusUseCase;
import com.devinolabs.uap.training.application.CreateTrainingPlanUseCase;
import com.devinolabs.uap.training.application.DeleteTrainingPlanUseCase;
import com.devinolabs.uap.training.application.GetTrainingPlanUseCase;
import com.devinolabs.uap.training.application.ListTrainingPlansUseCase;
import com.devinolabs.uap.training.application.TrainingPlanResult;
import com.devinolabs.uap.training.application.UpdateTrainingPlanCommand;
import com.devinolabs.uap.training.application.UpdateTrainingPlanUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@RestController
@RequestMapping("/api/v1/training/plans")
class TrainingPlanController {

	private final CreateTrainingPlanUseCase createTrainingPlanUseCase;
	private final ListTrainingPlansUseCase listTrainingPlansUseCase;
	private final GetTrainingPlanUseCase getTrainingPlanUseCase;
	private final UpdateTrainingPlanUseCase updateTrainingPlanUseCase;
	private final ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase;
	private final DeleteTrainingPlanUseCase deleteTrainingPlanUseCase;

	TrainingPlanController(
			CreateTrainingPlanUseCase createTrainingPlanUseCase,
			ListTrainingPlansUseCase listTrainingPlansUseCase,
			GetTrainingPlanUseCase getTrainingPlanUseCase,
			UpdateTrainingPlanUseCase updateTrainingPlanUseCase,
			ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase,
			DeleteTrainingPlanUseCase deleteTrainingPlanUseCase) {
		this.createTrainingPlanUseCase = Objects.requireNonNull(createTrainingPlanUseCase);
		this.listTrainingPlansUseCase = Objects.requireNonNull(listTrainingPlansUseCase);
		this.getTrainingPlanUseCase = Objects.requireNonNull(getTrainingPlanUseCase);
		this.updateTrainingPlanUseCase = Objects.requireNonNull(updateTrainingPlanUseCase);
		this.changeTrainingPlanStatusUseCase = Objects.requireNonNull(changeTrainingPlanStatusUseCase);
		this.deleteTrainingPlanUseCase = Objects.requireNonNull(deleteTrainingPlanUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	TrainingPlanResponse create(@Valid @RequestBody CreateTrainingPlanRequest request, Authentication authentication) {
		return toResponse(createTrainingPlanUseCase.execute(
				accountId(authentication),
				request.type(),
				request.customTypeName(),
				request.name(),
				request.description(),
				request.startDate(),
				request.endDate(),
				request.athleteSportId(),
				request.athleteGoalId(),
				request.defaultTrainingEnvironmentId()));
	}

	@GetMapping
	List<TrainingPlanResponse> list(
			@RequestParam(required = false) TrainingPlanStatus status,
			@RequestParam(required = false) TrainingPlanType planType,
			Authentication authentication) {
		return listTrainingPlansUseCase.execute(accountId(authentication), status, planType)
				.stream()
				.map(TrainingPlanController::toResponse)
				.toList();
	}

	@GetMapping("/{planId}")
	TrainingPlanResponse get(@PathVariable UUID planId, Authentication authentication) {
		return toResponse(getTrainingPlanUseCase.execute(accountId(authentication), TrainingPlanId.of(planId)));
	}

	@PatchMapping("/{planId}")
	TrainingPlanResponse update(
			@PathVariable UUID planId,
			@Valid @RequestBody UpdateTrainingPlanRequest request,
			Authentication authentication) {
		UpdateTrainingPlanCommand command = new UpdateTrainingPlanCommand(
				request.name() == null ? null : request.name().value(),
				request.name() != null,
				request.description() == null ? null : request.description().value(),
				request.description() != null,
				request.startDate() == null ? null : request.startDate().value(),
				request.startDate() != null,
				request.endDate() == null ? null : request.endDate().value(),
				request.endDate() != null,
				request.athleteSportId() == null ? null : request.athleteSportId().value(),
				request.athleteSportId() != null,
				request.athleteGoalId() == null ? null : request.athleteGoalId().value(),
				request.athleteGoalId() != null,
			request.defaultTrainingEnvironmentId() == null ? null : request.defaultTrainingEnvironmentId().value(),
			request.defaultTrainingEnvironmentId() != null);
		return toResponse(updateTrainingPlanUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				command));
	}

	@PatchMapping("/{planId}/status")
	TrainingPlanResponse changeStatus(
			@PathVariable UUID planId,
			@Valid @RequestBody TrainingPlanStatusRequest request,
			Authentication authentication) {
		return toResponse(changeTrainingPlanStatusUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				request.action()));
	}

	@DeleteMapping("/{planId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID planId, Authentication authentication) {
		deleteTrainingPlanUseCase.execute(accountId(authentication), TrainingPlanId.of(planId));
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

	static TrainingPlanResponse toResponse(TrainingPlanResult result) {
		return new TrainingPlanResponse(
				result.id().value(),
				result.type(),
				result.customTypeName(),
				result.name(),
				result.description(),
				result.status(),
				result.startDate(),
				result.endDate(),
				result.athleteSportId() == null ? null : result.athleteSportId().value(),
				result.athleteGoalId() == null ? null : result.athleteGoalId().value(),
				result.defaultTrainingEnvironmentId() == null
						? null
						: result.defaultTrainingEnvironmentId().value(),
				result.scheduleStartDate(),
				result.scheduleEndDate(),
				result.scheduleTimezone(),
				result.scheduleStatus(),
				result.recurrenceMode(),
				result.scheduleGeneratedThrough(),
				result.scheduleActivatedAt(),
				result.schedulePausedAt(),
				result.createdAt(),
				result.updatedAt());
	}

}
