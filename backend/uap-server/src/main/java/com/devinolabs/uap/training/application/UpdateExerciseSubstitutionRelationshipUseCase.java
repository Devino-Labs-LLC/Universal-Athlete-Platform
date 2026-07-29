package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;

@Service
public class UpdateExerciseSubstitutionRelationshipUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;
	private final Clock clock;

	public UpdateExerciseSubstitutionRelationshipUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseSubstitutionRelationshipRepository relationshipRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ExerciseSubstitutionRelationshipResult execute(
			AccountId accountId,
			ExerciseSubstitutionRelationshipId relationshipId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale) {
		Objects.requireNonNull(relationshipType, "relationshipType must not be null");
		Objects.requireNonNull(compatibilityLevel, "compatibilityLevel must not be null");
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseSubstitutionRelationship relationship = ExerciseSubstitutionRelationshipAccessPolicy.requireOwnedMutable(
				relationshipRepository, athleteId, relationshipId);
		relationship.update(relationshipType, compatibilityLevel, rationale, clock);
		return ExerciseDefinitionSupport.toRelationshipResult(relationshipRepository.save(relationship));
	}

}
