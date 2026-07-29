package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.KineticChainType;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

public record UpdateExerciseDefinitionRequest(
		PatchValue<String> canonicalName,
		PatchValue<ExerciseDefinitionCategory> category,
		PatchValue<ExerciseMetricMode> metricMode,
		PatchValue<MovementPattern> primaryMovementPattern,
		PatchValue<List<MovementPattern>> secondaryMovementPatterns,
		PatchValue<List<MuscleGroup>> primaryMuscleGroups,
		PatchValue<List<MuscleGroup>> secondaryMuscleGroups,
		PatchValue<List<EquipmentType>> requiredEquipment,
		PatchValue<List<EquipmentType>> optionalEquipment,
		PatchValue<ExerciseLaterality> laterality,
		PatchValue<KineticChainType> kineticChainType,
		PatchValue<ImpactLevel> impactLevel,
		PatchValue<ExerciseDifficulty> difficulty) {
}
