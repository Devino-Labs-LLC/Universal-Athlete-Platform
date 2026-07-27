package com.devinolabs.uap.training.infrastructure.web;

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
import com.devinolabs.uap.training.application.ArchiveAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.CreateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.GetExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.ListAccessibleExerciseDefinitionsUseCase;
import com.devinolabs.uap.training.application.UpdateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

@RestController
@RequestMapping("/api/v1/training/exercise-definitions")
class ExerciseDefinitionController {

	private final CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;
	private final ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase;
	private final GetExerciseDefinitionUseCase getExerciseDefinitionUseCase;
	private final UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase;
	private final ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase;

	ExerciseDefinitionController(
			CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase,
			ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase,
			GetExerciseDefinitionUseCase getExerciseDefinitionUseCase,
			UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase,
			ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase) {
		this.createAthleteExerciseDefinitionUseCase = Objects.requireNonNull(createAthleteExerciseDefinitionUseCase);
		this.listAccessibleExerciseDefinitionsUseCase =
				Objects.requireNonNull(listAccessibleExerciseDefinitionsUseCase);
		this.getExerciseDefinitionUseCase = Objects.requireNonNull(getExerciseDefinitionUseCase);
		this.updateAthleteExerciseDefinitionUseCase = Objects.requireNonNull(updateAthleteExerciseDefinitionUseCase);
		this.archiveAthleteExerciseDefinitionUseCase = Objects.requireNonNull(archiveAthleteExerciseDefinitionUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ExerciseDefinitionResponse create(
			@Valid @RequestBody CreateExerciseDefinitionRequest request,
			Authentication authentication) {
		return ExerciseDefinitionResponse.from(createAthleteExerciseDefinitionUseCase.execute(
				accountId(authentication), request.canonicalName()));
	}

	@GetMapping
	ExerciseDefinitionPageResponse list(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) ExerciseDefinitionScope scope,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return ExerciseDefinitionPageResponse.from(listAccessibleExerciseDefinitionsUseCase.execute(
				accountId(authentication), name, scope, page, size));
	}

	@GetMapping("/{exerciseDefinitionId}")
	ExerciseDefinitionResponse get(
			@PathVariable UUID exerciseDefinitionId,
			Authentication authentication) {
		return ExerciseDefinitionResponse.from(getExerciseDefinitionUseCase.execute(
				accountId(authentication), ExerciseDefinitionId.of(exerciseDefinitionId)));
	}

	@PatchMapping("/{exerciseDefinitionId}")
	ExerciseDefinitionResponse update(
			@PathVariable UUID exerciseDefinitionId,
			@Valid @RequestBody UpdateExerciseDefinitionRequest request,
			Authentication authentication) {
		return ExerciseDefinitionResponse.from(updateAthleteExerciseDefinitionUseCase.execute(
				accountId(authentication),
				ExerciseDefinitionId.of(exerciseDefinitionId),
				request.canonicalName()));
	}

	/**
	 * Archives instead of deleting: the definition is the identity an athlete's history and personal
	 * records hang from, so it is retired from selection rather than removed.
	 */
	@DeleteMapping("/{exerciseDefinitionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(
			@PathVariable UUID exerciseDefinitionId,
			Authentication authentication) {
		archiveAthleteExerciseDefinitionUseCase.execute(
				accountId(authentication), ExerciseDefinitionId.of(exerciseDefinitionId));
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}
