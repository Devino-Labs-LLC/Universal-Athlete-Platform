package com.devinolabs.uap.training.application;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

/**
 * Optional catalogue filters applied with logical AND when listing active accessible definitions.
 */
public record ExerciseDefinitionFilters(
		String nameContains,
		ExerciseDefinitionScope scope,
		ExerciseDefinitionCategory category,
		ExerciseMetricMode metricMode,
		MovementPattern movementPattern,
		MuscleGroup muscleGroup,
		EquipmentType equipment,
		ExerciseLaterality laterality,
		ImpactLevel impactLevel,
		ExerciseDifficulty difficulty) {

	public static ExerciseDefinitionFilters of(
			String nameContains,
			ExerciseDefinitionScope scope,
			ExerciseDefinitionCategory category,
			ExerciseMetricMode metricMode,
			MovementPattern movementPattern,
			MuscleGroup muscleGroup,
			EquipmentType equipment,
			ExerciseLaterality laterality,
			ImpactLevel impactLevel,
			ExerciseDifficulty difficulty) {
		return new ExerciseDefinitionFilters(
				nameContains,
				scope,
				category,
				metricMode,
				movementPattern,
				muscleGroup,
				equipment,
				laterality,
				impactLevel,
				difficulty);
	}

}
