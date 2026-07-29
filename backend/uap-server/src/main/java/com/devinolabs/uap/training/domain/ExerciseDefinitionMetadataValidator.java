package com.devinolabs.uap.training.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

final class ExerciseDefinitionMetadataValidator {

	private ExerciseDefinitionMetadataValidator() {
	}

	static void validate(ExerciseDefinitionMetadata metadata) {
		Objects.requireNonNull(metadata, "metadata must not be null");
		if (metadata.secondaryMovementPatterns().contains(metadata.primaryMovementPattern())) {
			throw new ExerciseMetadataPrimarySecondaryConflictException(
					"Primary movement pattern must not also appear as a secondary pattern");
		}
		Set<MuscleGroup> primaryMuscles = EnumSet.copyOf(metadata.primaryMuscleGroups().isEmpty()
				? EnumSet.noneOf(MuscleGroup.class)
				: EnumSet.copyOf(metadata.primaryMuscleGroups()));
		for (MuscleGroup secondary : metadata.secondaryMuscleGroups()) {
			if (primaryMuscles.contains(secondary)) {
				throw new ExerciseMetadataPrimarySecondaryConflictException(
						"Primary and secondary muscle groups must not overlap");
			}
		}
		if (primaryMuscles.contains(MuscleGroup.FULL_BODY) && primaryMuscles.size() > 1) {
			throw new InvalidExerciseDefinitionMetadataException(
					"FULL_BODY cannot be combined with other primary muscle groups");
		}
		if (requiresPrimaryMuscle(metadata.category()) && metadata.primaryMuscleGroups().isEmpty()) {
			throw new InvalidExerciseDefinitionMetadataException(
					"STRENGTH, POWER and PLYOMETRIC definitions require at least one primary muscle group");
		}
		Set<EquipmentType> required = EnumSet.copyOf(metadata.requiredEquipment().isEmpty()
				? EnumSet.noneOf(EquipmentType.class)
				: EnumSet.copyOf(metadata.requiredEquipment()));
		for (EquipmentType optional : metadata.optionalEquipment()) {
			if (required.contains(optional)) {
				throw new ExerciseEquipmentRequiredOptionalConflictException(
						"Required and optional equipment must not overlap");
			}
		}
	}

	private static boolean requiresPrimaryMuscle(ExerciseDefinitionCategory category) {
		return category == ExerciseDefinitionCategory.STRENGTH
				|| category == ExerciseDefinitionCategory.POWER
				|| category == ExerciseDefinitionCategory.PLYOMETRIC;
	}

}
