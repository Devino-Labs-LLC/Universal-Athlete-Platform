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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ChangeWorkoutDayStatusUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutDayUseCase;
import com.devinolabs.uap.training.application.DeleteWorkoutDayUseCase;
import com.devinolabs.uap.training.application.GetWorkoutDayUseCase;
import com.devinolabs.uap.training.application.ListWorkoutDaysUseCase;
import com.devinolabs.uap.training.application.ReorderWorkoutDaysUseCase;
import com.devinolabs.uap.training.application.UpdateWorkoutDayCommand;
import com.devinolabs.uap.training.application.UpdateWorkoutDayUseCase;
import com.devinolabs.uap.training.application.WorkoutDayResult;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;

@RestController
@RequestMapping("/api/v1/training/plans/{planId}/days")
class WorkoutDayController {

	private final CreateWorkoutDayUseCase createWorkoutDayUseCase;
	private final ListWorkoutDaysUseCase listWorkoutDaysUseCase;
	private final GetWorkoutDayUseCase getWorkoutDayUseCase;
	private final UpdateWorkoutDayUseCase updateWorkoutDayUseCase;
	private final ReorderWorkoutDaysUseCase reorderWorkoutDaysUseCase;
	private final ChangeWorkoutDayStatusUseCase changeWorkoutDayStatusUseCase;
	private final DeleteWorkoutDayUseCase deleteWorkoutDayUseCase;

	WorkoutDayController(
			CreateWorkoutDayUseCase createWorkoutDayUseCase,
			ListWorkoutDaysUseCase listWorkoutDaysUseCase,
			GetWorkoutDayUseCase getWorkoutDayUseCase,
			UpdateWorkoutDayUseCase updateWorkoutDayUseCase,
			ReorderWorkoutDaysUseCase reorderWorkoutDaysUseCase,
			ChangeWorkoutDayStatusUseCase changeWorkoutDayStatusUseCase,
			DeleteWorkoutDayUseCase deleteWorkoutDayUseCase) {
		this.createWorkoutDayUseCase = Objects.requireNonNull(createWorkoutDayUseCase);
		this.listWorkoutDaysUseCase = Objects.requireNonNull(listWorkoutDaysUseCase);
		this.getWorkoutDayUseCase = Objects.requireNonNull(getWorkoutDayUseCase);
		this.updateWorkoutDayUseCase = Objects.requireNonNull(updateWorkoutDayUseCase);
		this.reorderWorkoutDaysUseCase = Objects.requireNonNull(reorderWorkoutDaysUseCase);
		this.changeWorkoutDayStatusUseCase = Objects.requireNonNull(changeWorkoutDayStatusUseCase);
		this.deleteWorkoutDayUseCase = Objects.requireNonNull(deleteWorkoutDayUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	WorkoutDayResponse create(
			@PathVariable UUID planId,
			@Valid @RequestBody CreateWorkoutDayRequest request,
			Authentication authentication) {
		return toResponse(createWorkoutDayUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				request.title(),
				request.description(),
				request.scheduledDay(),
				request.plannedStartTime(),
				request.expectedDurationMinutes(),
				request.displayOrder()));
	}

	@GetMapping
	List<WorkoutDayResponse> list(@PathVariable UUID planId, Authentication authentication) {
		return listWorkoutDaysUseCase.execute(accountId(authentication), TrainingPlanId.of(planId))
				.stream()
				.map(WorkoutDayController::toResponse)
				.toList();
	}

	@GetMapping("/{dayId}")
	WorkoutDayResponse get(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			Authentication authentication) {
		return toResponse(getWorkoutDayUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId)));
	}

	@PatchMapping("/{dayId}")
	WorkoutDayResponse update(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@Valid @RequestBody UpdateWorkoutDayRequest request,
			Authentication authentication) {
		UpdateWorkoutDayCommand command = new UpdateWorkoutDayCommand(
				request.title() == null ? null : request.title().value(),
				request.title() != null,
				request.description() == null ? null : request.description().value(),
				request.description() != null,
				request.scheduledDay() == null ? null : request.scheduledDay().value(),
				request.scheduledDay() != null,
				request.plannedStartTime() == null ? null : request.plannedStartTime().value(),
				request.plannedStartTime() != null,
				request.expectedDurationMinutes() == null ? null : request.expectedDurationMinutes().value(),
				request.expectedDurationMinutes() != null,
				request.displayOrder() == null ? null : request.displayOrder().value(),
				request.displayOrder() != null);
		return toResponse(updateWorkoutDayUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				command));
	}

	@PatchMapping("/{dayId}/status")
	WorkoutDayResponse changeStatus(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			@Valid @RequestBody WorkoutDayStatusRequest request,
			Authentication authentication) {
		return toResponse(changeWorkoutDayStatusUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId),
				request.action()));
	}

	@PutMapping("/order")
	List<WorkoutDayResponse> reorder(
			@PathVariable UUID planId,
			@Valid @RequestBody ReorderWorkoutDaysRequest request,
			Authentication authentication) {
		return reorderWorkoutDaysUseCase
				.execute(accountId(authentication), TrainingPlanId.of(planId), request.dayIds())
				.stream()
				.map(WorkoutDayController::toResponse)
				.toList();
	}

	@DeleteMapping("/{dayId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(
			@PathVariable UUID planId,
			@PathVariable UUID dayId,
			Authentication authentication) {
		deleteWorkoutDayUseCase.execute(
				accountId(authentication),
				TrainingPlanId.of(planId),
				WorkoutDayId.of(dayId));
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

	private static WorkoutDayResponse toResponse(WorkoutDayResult result) {
		return new WorkoutDayResponse(
				result.id().value(),
				result.displayOrder(),
				result.title(),
				result.description(),
				result.scheduledDay(),
				result.plannedStartTime(),
				result.expectedDurationMinutes(),
				result.status(),
				result.createdAt(),
				result.updatedAt());
	}

}
