package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;

final class ExerciseDefinitionMetadataMapper {

	private ExerciseDefinitionMetadataMapper() {
	}

	static ExerciseDefinitionMetadata toDomain(ExerciseDefinitionMetadataRequest request) {
		return ExerciseDefinitionMetadata.of(
				request.category(),
				request.metricMode(),
				request.primaryMovementPattern(),
				request.secondaryMovementPatterns(),
				request.primaryMuscleGroups(),
				request.secondaryMuscleGroups(),
				request.requiredEquipment(),
				request.optionalEquipment(),
				request.laterality(),
				request.kineticChainType(),
				request.impactLevel(),
				request.difficulty());
	}

}
