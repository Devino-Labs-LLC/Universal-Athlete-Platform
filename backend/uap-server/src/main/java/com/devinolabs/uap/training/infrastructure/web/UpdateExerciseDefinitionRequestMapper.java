package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.UpdateAthleteExerciseDefinitionCommand;

final class UpdateExerciseDefinitionRequestMapper {

	private UpdateExerciseDefinitionRequestMapper() {
	}

	static UpdateAthleteExerciseDefinitionCommand toCommand(UpdateExerciseDefinitionRequest request) {
		return new UpdateAthleteExerciseDefinitionCommand(
				request.canonicalName() == null ? null : request.canonicalName().value(),
				request.canonicalName() != null,
				request.category() == null ? null : request.category().value(),
				request.category() != null,
				request.metricMode() == null ? null : request.metricMode().value(),
				request.metricMode() != null,
				request.primaryMovementPattern() == null ? null : request.primaryMovementPattern().value(),
				request.primaryMovementPattern() != null,
				request.secondaryMovementPatterns() == null ? null : request.secondaryMovementPatterns().value(),
				request.secondaryMovementPatterns() != null,
				request.primaryMuscleGroups() == null ? null : request.primaryMuscleGroups().value(),
				request.primaryMuscleGroups() != null,
				request.secondaryMuscleGroups() == null ? null : request.secondaryMuscleGroups().value(),
				request.secondaryMuscleGroups() != null,
				request.requiredEquipment() == null ? null : request.requiredEquipment().value(),
				request.requiredEquipment() != null,
				request.optionalEquipment() == null ? null : request.optionalEquipment().value(),
				request.optionalEquipment() != null,
				request.laterality() == null ? null : request.laterality().value(),
				request.laterality() != null,
				request.kineticChainType() == null ? null : request.kineticChainType().value(),
				request.kineticChainType() != null,
				request.impactLevel() == null ? null : request.impactLevel().value(),
				request.impactLevel() != null,
				request.difficulty() == null ? null : request.difficulty().value(),
				request.difficulty() != null);
	}

}
