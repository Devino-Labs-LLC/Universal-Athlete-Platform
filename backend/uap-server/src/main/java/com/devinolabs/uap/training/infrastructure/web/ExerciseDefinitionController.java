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
import com.devinolabs.uap.training.application.ArchiveAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.ArchiveExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.CreateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.CreateExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.GetExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.GetExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.ListAccessibleExerciseDefinitionsUseCase;
import com.devinolabs.uap.training.application.ListExerciseSubstitutionCandidatesUseCase;
import com.devinolabs.uap.training.application.GetExerciseEnvironmentCompatibilityUseCase;
import com.devinolabs.uap.training.application.UpdateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.UpdateExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

@RestController
@RequestMapping("/api/v1/training/exercise-definitions")
class ExerciseDefinitionController {

	private final CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;
	private final ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase;
	private final GetExerciseDefinitionUseCase getExerciseDefinitionUseCase;
	private final UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase;
	private final ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase;
	private final CreateExerciseSubstitutionRelationshipUseCase createExerciseSubstitutionRelationshipUseCase;
	private final ListExerciseSubstitutionCandidatesUseCase listExerciseSubstitutionCandidatesUseCase;
	private final GetExerciseEnvironmentCompatibilityUseCase getExerciseEnvironmentCompatibilityUseCase;

	ExerciseDefinitionController(
			CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase,
			ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase,
			GetExerciseDefinitionUseCase getExerciseDefinitionUseCase,
			UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase,
			ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase,
			CreateExerciseSubstitutionRelationshipUseCase createExerciseSubstitutionRelationshipUseCase,
			ListExerciseSubstitutionCandidatesUseCase listExerciseSubstitutionCandidatesUseCase,
			GetExerciseEnvironmentCompatibilityUseCase getExerciseEnvironmentCompatibilityUseCase) {
		this.createAthleteExerciseDefinitionUseCase = Objects.requireNonNull(createAthleteExerciseDefinitionUseCase);
		this.listAccessibleExerciseDefinitionsUseCase =
				Objects.requireNonNull(listAccessibleExerciseDefinitionsUseCase);
		this.getExerciseDefinitionUseCase = Objects.requireNonNull(getExerciseDefinitionUseCase);
		this.updateAthleteExerciseDefinitionUseCase = Objects.requireNonNull(updateAthleteExerciseDefinitionUseCase);
		this.archiveAthleteExerciseDefinitionUseCase = Objects.requireNonNull(archiveAthleteExerciseDefinitionUseCase);
		this.createExerciseSubstitutionRelationshipUseCase =
				Objects.requireNonNull(createExerciseSubstitutionRelationshipUseCase);
		this.listExerciseSubstitutionCandidatesUseCase =
				Objects.requireNonNull(listExerciseSubstitutionCandidatesUseCase);
		this.getExerciseEnvironmentCompatibilityUseCase =
				Objects.requireNonNull(getExerciseEnvironmentCompatibilityUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ExerciseDefinitionResponse create(
			@Valid @RequestBody CreateExerciseDefinitionRequest request,
			Authentication authentication) {
		return ExerciseDefinitionResponse.from(createAthleteExerciseDefinitionUseCase.execute(
				accountId(authentication),
				request.canonicalName(),
				ExerciseDefinitionMetadataMapper.toDomain(request.metadata())));
	}

	@GetMapping
	ExerciseDefinitionPageResponse list(
			@RequestParam(required = false) String name,
			@RequestParam(required = false) ExerciseDefinitionScope scope,
			@RequestParam(required = false) ExerciseDefinitionCategory category,
			@RequestParam(required = false) ExerciseMetricMode metricMode,
			@RequestParam(required = false) MovementPattern movementPattern,
			@RequestParam(required = false) MuscleGroup muscleGroup,
			@RequestParam(required = false) EquipmentType equipment,
			@RequestParam(required = false) ExerciseLaterality laterality,
			@RequestParam(required = false) ImpactLevel impactLevel,
			@RequestParam(required = false) ExerciseDifficulty difficulty,
			@RequestParam(required = false) Integer page,
			@RequestParam(required = false) Integer size,
			Authentication authentication) {
		return ExerciseDefinitionPageResponse.from(listAccessibleExerciseDefinitionsUseCase.execute(
				accountId(authentication),
				name,
				scope,
				category,
				metricMode,
				movementPattern,
				muscleGroup,
				equipment,
				laterality,
				impactLevel,
				difficulty,
				page,
				size));
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
				UpdateExerciseDefinitionRequestMapper.toCommand(request)));
	}

	@DeleteMapping("/{exerciseDefinitionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(
			@PathVariable UUID exerciseDefinitionId,
			Authentication authentication) {
		archiveAthleteExerciseDefinitionUseCase.execute(
				accountId(authentication), ExerciseDefinitionId.of(exerciseDefinitionId));
	}

	@PostMapping("/{sourceDefinitionId}/substitutions")
	@ResponseStatus(HttpStatus.CREATED)
	ExerciseSubstitutionRelationshipResponse createSubstitutionRelationship(
			@PathVariable UUID sourceDefinitionId,
			@Valid @RequestBody CreateExerciseSubstitutionRelationshipRequest request,
			Authentication authentication) {
		return ExerciseSubstitutionRelationshipResponse.from(
				createExerciseSubstitutionRelationshipUseCase.execute(
						accountId(authentication),
						ExerciseDefinitionId.of(sourceDefinitionId),
						ExerciseDefinitionId.of(request.targetExerciseDefinitionId()),
						request.relationshipType(),
						request.compatibilityLevel(),
						request.rationale()));
	}

	@GetMapping("/{sourceDefinitionId}/substitution-candidates")
	List<ExerciseSubstitutionCandidateResponse> substitutionCandidates(
			@PathVariable UUID sourceDefinitionId,
			@RequestParam(required = false) List<EquipmentType> equipment,
			@RequestParam(required = false) UUID trainingEnvironmentId,
			Authentication authentication) {
		return listExerciseSubstitutionCandidatesUseCase
				.execute(
						accountId(authentication),
						ExerciseDefinitionId.of(sourceDefinitionId),
						equipment == null ? List.of() : equipment,
						trainingEnvironmentId == null ? null : TrainingEnvironmentId.of(trainingEnvironmentId))
				.stream()
				.map(ExerciseSubstitutionCandidateResponse::from)
				.toList();
	}

	@GetMapping("/{exerciseDefinitionId}/environment-compatibility/{environmentId}")
	ExerciseEnvironmentCompatibilityResponse environmentCompatibility(
			@PathVariable UUID exerciseDefinitionId,
			@PathVariable UUID environmentId,
			Authentication authentication) {
		return ExerciseEnvironmentCompatibilityResponse.from(getExerciseEnvironmentCompatibilityUseCase.execute(
				accountId(authentication),
				ExerciseDefinitionId.of(exerciseDefinitionId),
				TrainingEnvironmentId.of(environmentId)));
	}

	private static AccountId accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return AccountId.of(principal.accountUuid());
	}

}
