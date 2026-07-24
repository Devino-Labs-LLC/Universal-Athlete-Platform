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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.athlete.application.AthleteGoalResult;
import com.devinolabs.uap.athlete.application.ChangeAthleteGoalStatusUseCase;
import com.devinolabs.uap.athlete.application.CreateAthleteGoalUseCase;
import com.devinolabs.uap.athlete.application.DeleteAthleteGoalUseCase;
import com.devinolabs.uap.athlete.application.GetCurrentAthleteGoalUseCase;
import com.devinolabs.uap.athlete.application.ListCurrentAthleteGoalsUseCase;
import com.devinolabs.uap.athlete.application.UpdateAthleteGoalCommand;
import com.devinolabs.uap.athlete.application.UpdateAthleteGoalUseCase;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalType;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

@RestController
@RequestMapping("/api/v1/athletes/me/goals")
class AthleteGoalController {

	private final CreateAthleteGoalUseCase createAthleteGoalUseCase;
	private final ListCurrentAthleteGoalsUseCase listCurrentAthleteGoalsUseCase;
	private final GetCurrentAthleteGoalUseCase getCurrentAthleteGoalUseCase;
	private final UpdateAthleteGoalUseCase updateAthleteGoalUseCase;
	private final ChangeAthleteGoalStatusUseCase changeAthleteGoalStatusUseCase;
	private final DeleteAthleteGoalUseCase deleteAthleteGoalUseCase;

	AthleteGoalController(
			CreateAthleteGoalUseCase createAthleteGoalUseCase,
			ListCurrentAthleteGoalsUseCase listCurrentAthleteGoalsUseCase,
			GetCurrentAthleteGoalUseCase getCurrentAthleteGoalUseCase,
			UpdateAthleteGoalUseCase updateAthleteGoalUseCase,
			ChangeAthleteGoalStatusUseCase changeAthleteGoalStatusUseCase,
			DeleteAthleteGoalUseCase deleteAthleteGoalUseCase) {
		this.createAthleteGoalUseCase = Objects.requireNonNull(createAthleteGoalUseCase);
		this.listCurrentAthleteGoalsUseCase = Objects.requireNonNull(listCurrentAthleteGoalsUseCase);
		this.getCurrentAthleteGoalUseCase = Objects.requireNonNull(getCurrentAthleteGoalUseCase);
		this.updateAthleteGoalUseCase = Objects.requireNonNull(updateAthleteGoalUseCase);
		this.changeAthleteGoalStatusUseCase = Objects.requireNonNull(changeAthleteGoalStatusUseCase);
		this.deleteAthleteGoalUseCase = Objects.requireNonNull(deleteAthleteGoalUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	AthleteGoalResponse create(@Valid @RequestBody CreateAthleteGoalRequest request, Authentication authentication) {
		AthleteGoalResult result = createAthleteGoalUseCase.execute(
				accountId(authentication),
				request.goalType(),
				request.customGoalName(),
				request.title(),
				request.description(),
				request.priority() == null ? GoalPriority.MEDIUM : request.priority(),
				request.targetValue(),
				request.targetUnit(),
				request.customTargetUnit(),
				request.targetDate(),
				request.athleteSportId() == null ? null : AthleteSportId.of(request.athleteSportId()));
		return toResponse(result);
	}

	@GetMapping
	List<AthleteGoalResponse> list(
			@RequestParam(required = false) GoalStatus status,
			@RequestParam(required = false) GoalType goalType,
			Authentication authentication) {
		return listCurrentAthleteGoalsUseCase.execute(accountId(authentication), status, goalType).stream()
				.map(AthleteGoalController::toResponse)
				.toList();
	}

	@GetMapping("/{goalId}")
	AthleteGoalResponse get(@PathVariable UUID goalId, Authentication authentication) {
		return toResponse(getCurrentAthleteGoalUseCase.execute(accountId(authentication), AthleteGoalId.of(goalId)));
	}

	@PatchMapping("/{goalId}")
	AthleteGoalResponse update(
			@PathVariable UUID goalId,
			@Valid @RequestBody UpdateAthleteGoalRequest request,
			Authentication authentication) {
		UpdateAthleteGoalCommand command = new UpdateAthleteGoalCommand(
				request.title() == null ? null : request.title().value(),
				request.title() != null,
				request.description() == null ? null : request.description().value(),
				request.description() != null,
				request.priority() == null ? null : request.priority().value(),
				request.priority() != null,
				request.targetValue() == null ? null : request.targetValue().value(),
				request.targetValue() != null,
				request.targetUnit() == null ? null : request.targetUnit().value(),
				request.targetUnit() != null,
				request.customTargetUnit() == null ? null : request.customTargetUnit().value(),
				request.customTargetUnit() != null,
				request.targetDate() == null ? null : request.targetDate().value(),
				request.targetDate() != null,
				request.athleteSportId() == null ? null : request.athleteSportId().value(),
				request.athleteSportId() != null);
		return toResponse(updateAthleteGoalUseCase.execute(
				accountId(authentication),
				AthleteGoalId.of(goalId),
				command));
	}

	@PatchMapping("/{goalId}/status")
	AthleteGoalResponse changeStatus(
			@PathVariable UUID goalId,
			@Valid @RequestBody ChangeAthleteGoalStatusRequest request,
			Authentication authentication) {
		return toResponse(changeAthleteGoalStatusUseCase.execute(
				accountId(authentication),
				AthleteGoalId.of(goalId),
				request.action()));
	}

	@DeleteMapping("/{goalId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void delete(@PathVariable UUID goalId, Authentication authentication) {
		deleteAthleteGoalUseCase.execute(accountId(authentication), AthleteGoalId.of(goalId));
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

	private static AthleteGoalResponse toResponse(AthleteGoalResult result) {
		return new AthleteGoalResponse(
				result.id().value(),
				result.goalType(),
				result.customGoalName(),
				result.title(),
				result.description(),
				result.priority(),
				result.status(),
				result.targetValue(),
				result.targetUnit(),
				result.customTargetUnit(),
				result.targetDate(),
				result.athleteSportId() == null ? null : result.athleteSportId().value(),
				result.createdAt(),
				result.updatedAt(),
				result.completedAt());
	}

}
