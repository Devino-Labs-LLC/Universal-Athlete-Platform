package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;

final class ExerciseSubstitutionRelationshipPersistenceMapper {

	private ExerciseSubstitutionRelationshipPersistenceMapper() {
	}

	static ExerciseSubstitutionRelationshipJpaEntity toEntity(
			ExerciseSubstitutionRelationship relationship,
			boolean isNew) {
		return new ExerciseSubstitutionRelationshipJpaEntity(
				relationship.id().value(),
				relationship.ownerAthleteId() == null ? null : relationship.ownerAthleteId().value(),
				relationship.sourceExerciseDefinitionId().value(),
				relationship.targetExerciseDefinitionId().value(),
				relationship.relationshipType(),
				relationship.compatibilityLevel(),
				relationship.rationale(),
				relationship.active(),
				relationship.archivedAt(),
				relationship.createdAt(),
				relationship.updatedAt(),
				relationship.version(),
				isNew);
	}

	static ExerciseSubstitutionRelationship toDomain(ExerciseSubstitutionRelationshipJpaEntity entity) {
		return ExerciseSubstitutionRelationship.rehydrate(
				ExerciseSubstitutionRelationshipId.of(entity.getId()),
				entity.getOwnerAthleteId() == null ? null : AthleteId.of(entity.getOwnerAthleteId()),
				ExerciseDefinitionId.of(entity.getSourceExerciseDefinitionId()),
				ExerciseDefinitionId.of(entity.getTargetExerciseDefinitionId()),
				entity.getRelationshipType(),
				entity.getCompatibilityLevel(),
				entity.getRationale(),
				entity.isActive(),
				entity.getArchivedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
