package com.devinolabs.uap.training.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;

@Service
public class ListExerciseSubstitutionCandidatesUseCase {

	private static final Comparator<ExerciseSubstitutionCompatibility> COMPATIBILITY_ORDER =
			Comparator.comparingInt(ListExerciseSubstitutionCandidatesUseCase::compatibilityRank);

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;

	public ListExerciseSubstitutionCandidatesUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
	}

	@Transactional(readOnly = true)
	public List<ExerciseSubstitutionCandidateResult> execute(
			AccountId accountId,
			ExerciseDefinitionId sourceDefinitionId,
			List<EquipmentType> availableEquipment) {
		Objects.requireNonNull(sourceDefinitionId, "sourceDefinitionId must not be null");
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinitionSupport.requireAccessible(exerciseDefinitionRepository, athleteId, sourceDefinitionId);
		List<ExerciseSubstitutionRelationship> relationships = relationshipRepository.findActiveBySourceDefinitionId(
				sourceDefinitionId, athleteId);
		if (relationships.isEmpty()) {
			return List.of();
		}
		List<ExerciseDefinitionId> targetIds = relationships.stream()
				.map(ExerciseSubstitutionRelationship::targetExerciseDefinitionId)
				.distinct()
				.toList();
		Map<ExerciseDefinitionId, ExerciseDefinition> targetsById = exerciseDefinitionRepository
				.findAllByIds(targetIds)
				.stream()
				.collect(Collectors.toMap(ExerciseDefinition::id, Function.identity()));
		List<ExerciseSubstitutionCandidateResult> candidates = new ArrayList<>();
		for (ExerciseSubstitutionRelationship relationship : relationships) {
			ExerciseDefinition target = targetsById.get(relationship.targetExerciseDefinitionId());
			if (target == null || !ExerciseDefinitionAccessPolicy.isSelectableForPrescription(athleteId, target)) {
				continue;
			}
			if (!EquipmentCompatibilityEvaluator.isCompatible(
					target.metadata().requiredEquipment(), availableEquipment)) {
				continue;
			}
			candidates.add(new ExerciseSubstitutionCandidateResult(
					relationship.id(),
					target.id(),
					target.canonicalName(),
					relationship.relationshipType(),
					relationship.compatibilityLevel(),
					relationship.rationale()));
		}
		candidates.sort(Comparator
				.comparing(ExerciseSubstitutionCandidateResult::compatibilityLevel, COMPATIBILITY_ORDER)
				.thenComparing(ExerciseSubstitutionCandidateResult::relationshipType)
				.thenComparing(ExerciseSubstitutionCandidateResult::targetCanonicalName,
						String.CASE_INSENSITIVE_ORDER)
				.thenComparing(candidate -> candidate.targetExerciseDefinitionId().value()));
		return candidates;
	}

	private static int compatibilityRank(ExerciseSubstitutionCompatibility compatibility) {
		return switch (compatibility) {
			case HIGH -> 0;
			case MODERATE -> 1;
			case CONDITIONAL -> 2;
		};
	}

}
