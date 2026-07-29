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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.ArchiveExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.GetExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.UpdateExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;

@RestController
@RequestMapping("/api/v1/training/exercise-substitution-relationships")
class ExerciseSubstitutionRelationshipController {

	private final GetExerciseSubstitutionRelationshipUseCase getExerciseSubstitutionRelationshipUseCase;
	private final UpdateExerciseSubstitutionRelationshipUseCase updateExerciseSubstitutionRelationshipUseCase;
	private final ArchiveExerciseSubstitutionRelationshipUseCase archiveExerciseSubstitutionRelationshipUseCase;

	ExerciseSubstitutionRelationshipController(
			GetExerciseSubstitutionRelationshipUseCase getExerciseSubstitutionRelationshipUseCase,
			UpdateExerciseSubstitutionRelationshipUseCase updateExerciseSubstitutionRelationshipUseCase,
			ArchiveExerciseSubstitutionRelationshipUseCase archiveExerciseSubstitutionRelationshipUseCase) {
		this.getExerciseSubstitutionRelationshipUseCase =
				Objects.requireNonNull(getExerciseSubstitutionRelationshipUseCase);
		this.updateExerciseSubstitutionRelationshipUseCase =
				Objects.requireNonNull(updateExerciseSubstitutionRelationshipUseCase);
		this.archiveExerciseSubstitutionRelationshipUseCase =
				Objects.requireNonNull(archiveExerciseSubstitutionRelationshipUseCase);
	}

	@GetMapping("/{relationshipId}")
	ExerciseSubstitutionRelationshipResponse get(
			@PathVariable UUID relationshipId,
			Authentication authentication) {
		return ExerciseSubstitutionRelationshipResponse.from(getExerciseSubstitutionRelationshipUseCase.execute(
				accountId(authentication), ExerciseSubstitutionRelationshipId.of(relationshipId)));
	}

	@PatchMapping("/{relationshipId}")
	ExerciseSubstitutionRelationshipResponse update(
			@PathVariable UUID relationshipId,
			@Valid @RequestBody UpdateExerciseSubstitutionRelationshipRequest request,
			Authentication authentication) {
		return ExerciseSubstitutionRelationshipResponse.from(updateExerciseSubstitutionRelationshipUseCase.execute(
				accountId(authentication),
				ExerciseSubstitutionRelationshipId.of(relationshipId),
				request.relationshipType(),
				request.compatibilityLevel(),
				request.rationale()));
	}

	@DeleteMapping("/{relationshipId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID relationshipId, Authentication authentication) {
		archiveExerciseSubstitutionRelationshipUseCase.execute(
				accountId(authentication), ExerciseSubstitutionRelationshipId.of(relationshipId));
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}
