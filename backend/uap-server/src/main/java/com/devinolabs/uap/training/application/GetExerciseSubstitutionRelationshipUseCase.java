package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;

@Service
public class GetExerciseSubstitutionRelationshipUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;

	public GetExerciseSubstitutionRelationshipUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseSubstitutionRelationshipRepository relationshipRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
	}

	@Transactional(readOnly = true)
	public ExerciseSubstitutionRelationshipResult execute(
			AccountId accountId,
			ExerciseSubstitutionRelationshipId relationshipId) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return ExerciseDefinitionSupport.toRelationshipResult(
				ExerciseSubstitutionRelationshipAccessPolicy.requireAccessible(
						relationshipRepository, athleteId, relationshipId));
	}

}
