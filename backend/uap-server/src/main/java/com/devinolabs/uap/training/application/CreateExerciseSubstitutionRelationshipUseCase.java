package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

@Service
public class CreateExerciseSubstitutionRelationshipUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public CreateExerciseSubstitutionRelationshipUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ExerciseSubstitutionRelationshipResult execute(
			AccountId accountId,
			ExerciseDefinitionId sourceDefinitionId,
			ExerciseDefinitionId targetDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale) {
		Objects.requireNonNull(sourceDefinitionId, "sourceDefinitionId must not be null");
		Objects.requireNonNull(targetDefinitionId, "targetDefinitionId must not be null");
		Objects.requireNonNull(relationshipType, "relationshipType must not be null");
		Objects.requireNonNull(compatibilityLevel, "compatibilityLevel must not be null");
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinition source = ExerciseDefinitionAccessPolicy.requireSelectable(
				athleteId,
				ExerciseDefinitionSupport.requireAccessible(
						exerciseDefinitionRepository, athleteId, sourceDefinitionId));
		ExerciseDefinition target = ExerciseDefinitionAccessPolicy.requireSelectable(
				athleteId,
				ExerciseDefinitionSupport.requireAccessible(
						exerciseDefinitionRepository, athleteId, targetDefinitionId));
		ExerciseSubstitutionRelationshipAccessPolicy.assertCreatable(athleteId, source, target);
		ExerciseSubstitutionRelationship relationship = ExerciseSubstitutionRelationship.createOwned(
				ExerciseSubstitutionRelationshipId.generate(),
				athleteId,
				source.id(),
				target.id(),
				relationshipType,
				compatibilityLevel,
				rationale,
				clock);
		return ExerciseDefinitionSupport.toRelationshipResult(relationshipRepository.save(relationship));
	}

}
