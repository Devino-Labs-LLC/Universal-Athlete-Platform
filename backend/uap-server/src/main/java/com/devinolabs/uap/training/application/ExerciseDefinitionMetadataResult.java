package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.KineticChainType;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

public record ExerciseDefinitionMetadataResult(
		ExerciseDefinitionCategory category,
		ExerciseMetricMode metricMode,
		MovementPattern primaryMovementPattern,
		List<MovementPattern> secondaryMovementPatterns,
		List<MuscleGroup> primaryMuscleGroups,
		List<MuscleGroup> secondaryMuscleGroups,
		List<EquipmentType> requiredEquipment,
		List<EquipmentType> optionalEquipment,
		ExerciseLaterality laterality,
		KineticChainType kineticChainType,
		ImpactLevel impactLevel,
		ExerciseDifficulty difficulty) {

	public static ExerciseDefinitionMetadataResult from(ExerciseDefinitionMetadata metadata) {
		return new ExerciseDefinitionMetadataResult(
				metadata.category(),
				metadata.metricMode(),
				metadata.primaryMovementPattern(),
				metadata.secondaryMovementPatterns(),
				metadata.primaryMuscleGroups(),
				metadata.secondaryMuscleGroups(),
				metadata.requiredEquipment(),
				metadata.optionalEquipment(),
				metadata.laterality(),
				metadata.kineticChainType(),
				metadata.impactLevel(),
				metadata.difficulty());
	}

}
