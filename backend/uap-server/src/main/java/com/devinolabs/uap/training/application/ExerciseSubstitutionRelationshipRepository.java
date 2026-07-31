package com.devinolabs.uap.training.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;

public interface ExerciseSubstitutionRelationshipRepository {

	ExerciseSubstitutionRelationship save(ExerciseSubstitutionRelationship relationship);

	Optional<ExerciseSubstitutionRelationship> findById(ExerciseSubstitutionRelationshipId id);

	Optional<ExerciseSubstitutionRelationship> findAccessible(
			ExerciseSubstitutionRelationshipId id,
			AthleteId athleteId);

	Optional<ExerciseSubstitutionRelationship> findActiveById(ExerciseSubstitutionRelationshipId id);

	List<ExerciseSubstitutionRelationship> findActiveBySourceDefinitionId(
			ExerciseDefinitionId sourceDefinitionId,
			AthleteId athleteId);

	List<ExerciseSubstitutionRelationship> findActiveBySourceDefinitionIds(
			Collection<ExerciseDefinitionId> sourceDefinitionIds,
			AthleteId athleteId);

}
