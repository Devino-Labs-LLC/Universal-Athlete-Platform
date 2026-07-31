package com.devinolabs.uap;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.KineticChainType;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

/**
 * Sensible catalogue metadata defaults for tests that create exercise definitions.
 */
public final class ExerciseDefinitionMetadataFixtures {

	private ExerciseDefinitionMetadataFixtures() {
	}

	public static ExerciseDefinitionMetadata defaultCustom() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.SQUAT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
				List.of(),
				List.of(),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

	public static ExerciseDefinitionMetadata backSquat() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.SQUAT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
				List.of(MuscleGroup.HAMSTRINGS, MuscleGroup.SPINAL_ERECTORS),
				List.of(EquipmentType.BARBELL, EquipmentType.SQUAT_RACK),
				List.of(EquipmentType.WEIGHT_PLATE),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

	public static ExerciseDefinitionMetadata gobletSquat() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.SQUAT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
				List.of(MuscleGroup.HAMSTRINGS),
				List.of(EquipmentType.DUMBBELL),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.BEGINNER);
	}

	public static ExerciseDefinitionMetadata legPress() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.SQUAT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
				List.of(MuscleGroup.HAMSTRINGS),
				List.of(EquipmentType.PLATE_LOADED_MACHINE),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.BEGINNER);
	}

	public static ExerciseDefinitionMetadata hotelDumbbellSquat() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.SQUAT,
				List.of(),
				List.of(MuscleGroup.QUADRICEPS, MuscleGroup.GLUTES),
				List.of(MuscleGroup.HAMSTRINGS),
				List.of(EquipmentType.DUMBBELL),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

	public static ExerciseDefinitionMetadata pullUp() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.VERTICAL_PULL,
				List.of(),
				List.of(MuscleGroup.LATS, MuscleGroup.BICEPS),
				List.of(MuscleGroup.SHOULDERS),
				List.of(EquipmentType.PULL_UP_BAR),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.CLOSED_CHAIN,
				ImpactLevel.NO_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

	public static ExerciseDefinitionMetadata dumbbellBenchPress() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.HORIZONTAL_PUSH,
				List.of(),
				List.of(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
				List.of(MuscleGroup.SHOULDERS),
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.OPEN_CHAIN,
				ImpactLevel.NO_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

}
