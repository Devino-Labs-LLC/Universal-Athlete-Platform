package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

public record UpdateAthleteExerciseDefinitionCommand(
		String canonicalName,
		boolean canonicalNamePresent,
		ExerciseDefinitionCategory category,
		boolean categoryPresent,
		ExerciseMetricMode metricMode,
		boolean metricModePresent,
		MovementPattern primaryMovementPattern,
		boolean primaryMovementPatternPresent,
		List<MovementPattern> secondaryMovementPatterns,
		boolean secondaryMovementPatternsPresent,
		List<MuscleGroup> primaryMuscleGroups,
		boolean primaryMuscleGroupsPresent,
		List<MuscleGroup> secondaryMuscleGroups,
		boolean secondaryMuscleGroupsPresent,
		List<EquipmentType> requiredEquipment,
		boolean requiredEquipmentPresent,
		List<EquipmentType> optionalEquipment,
		boolean optionalEquipmentPresent,
		ExerciseLaterality laterality,
		boolean lateralityPresent,
		com.devinolabs.uap.training.domain.KineticChainType kineticChainType,
		boolean kineticChainTypePresent,
		ImpactLevel impactLevel,
		boolean impactLevelPresent,
		ExerciseDifficulty difficulty,
		boolean difficultyPresent) {

	public static UpdateAthleteExerciseDefinitionCommand renameOnly(String canonicalName) {
		return new UpdateAthleteExerciseDefinitionCommand(
				canonicalName,
				true,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false,
				null,
				false);
	}

	public boolean hasMetadataPatch() {
		return categoryPresent
				|| metricModePresent
				|| primaryMovementPatternPresent
				|| secondaryMovementPatternsPresent
				|| primaryMuscleGroupsPresent
				|| secondaryMuscleGroupsPresent
				|| requiredEquipmentPresent
				|| optionalEquipmentPresent
				|| lateralityPresent
				|| kineticChainTypePresent
				|| impactLevelPresent
				|| difficultyPresent;
	}

}
