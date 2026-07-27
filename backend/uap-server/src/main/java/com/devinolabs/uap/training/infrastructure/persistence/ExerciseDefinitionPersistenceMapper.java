package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;

final class ExerciseDefinitionPersistenceMapper {

	private ExerciseDefinitionPersistenceMapper() {
	}

	static ExerciseDefinitionJpaEntity toEntity(ExerciseDefinition definition, boolean isNew) {
		return new ExerciseDefinitionJpaEntity(
				definition.id().value(),
				definition.scope(),
				definition.athleteId() == null ? null : definition.athleteId().value(),
				definition.canonicalName(),
				definition.normalizedName(),
				definition.active(),
				definition.archivedAt(),
				definition.createdAt(),
				definition.updatedAt(),
				definition.version(),
				isNew);
	}

	static ExerciseDefinition toDomain(ExerciseDefinitionJpaEntity entity) {
		return ExerciseDefinition.rehydrate(
				ExerciseDefinitionId.of(entity.getId()),
				entity.getScope(),
				entity.getAthleteId() == null ? null : AthleteId.of(entity.getAthleteId()),
				entity.getCanonicalName(),
				entity.getNormalizedName(),
				entity.isActive(),
				entity.getArchivedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
