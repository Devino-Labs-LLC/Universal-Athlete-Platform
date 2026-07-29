package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.LinkedHashSet;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;

final class ExerciseDefinitionPersistenceMapper {

	private ExerciseDefinitionPersistenceMapper() {
	}

	static ExerciseDefinitionJpaEntity toEntity(ExerciseDefinition definition, boolean isNew) {
		ExerciseDefinitionMetadata metadata = definition.metadata();
		return new ExerciseDefinitionJpaEntity(
				definition.id().value(),
				definition.scope(),
				definition.athleteId() == null ? null : definition.athleteId().value(),
				definition.canonicalName(),
				definition.normalizedName(),
				metadata.category(),
				metadata.metricMode(),
				metadata.primaryMovementPattern(),
				new LinkedHashSet<>(metadata.secondaryMovementPatterns()),
				new LinkedHashSet<>(metadata.primaryMuscleGroups()),
				new LinkedHashSet<>(metadata.secondaryMuscleGroups()),
				new LinkedHashSet<>(metadata.requiredEquipment()),
				new LinkedHashSet<>(metadata.optionalEquipment()),
				metadata.laterality(),
				metadata.kineticChainType(),
				metadata.impactLevel(),
				metadata.difficulty(),
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
				toMetadata(entity),
				entity.isActive(),
				entity.getArchivedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	private static ExerciseDefinitionMetadata toMetadata(ExerciseDefinitionJpaEntity entity) {
		return ExerciseDefinitionMetadata.of(
				entity.getCategory(),
				entity.getMetricMode(),
				entity.getPrimaryMovementPattern(),
				entity.getSecondaryMovementPatterns(),
				entity.getPrimaryMuscleGroups(),
				entity.getSecondaryMuscleGroups(),
				entity.getRequiredEquipment(),
				entity.getOptionalEquipment(),
				entity.getLaterality(),
				entity.getKineticChainType(),
				entity.getImpactLevel(),
				entity.getDifficulty());
	}

}
