package com.devinolabs.uap;

import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;

/**
 * JSON helpers for exercise-definition HTTP tests.
 */
public final class ExerciseDefinitionHttpPayloads {

	private ExerciseDefinitionHttpPayloads() {
	}

	public static String createPayload(String canonicalName) {
		return createPayload(canonicalName, ExerciseDefinitionMetadataFixtures.defaultCustom());
	}

	public static String createPayload(String canonicalName, ExerciseDefinitionMetadata metadata) {
		return """
				{
				  "canonicalName": "%s",
				  "metadata": {
				    "category": "%s",
				    "metricMode": "%s",
				    "primaryMovementPattern": "%s",
				    "secondaryMovementPatterns": [],
				    "primaryMuscleGroups": [%s],
				    "secondaryMuscleGroups": [%s],
				    "requiredEquipment": [%s],
				    "optionalEquipment": [%s],
				    "laterality": "%s",
				    "kineticChainType": "%s",
				    "impactLevel": "%s",
				    "difficulty": "%s"
				  }
				}
				""".formatted(
				escape(canonicalName),
				metadata.category(),
				metadata.metricMode(),
				metadata.primaryMovementPattern(),
				joinEnums(metadata.primaryMuscleGroups()),
				joinEnums(metadata.secondaryMuscleGroups()),
				joinEnums(metadata.requiredEquipment()),
				joinEnums(metadata.optionalEquipment()),
				metadata.laterality(),
				metadata.kineticChainType(),
				metadata.impactLevel(),
				metadata.difficulty());
	}

	private static String joinEnums(java.util.Collection<? extends Enum<?>> values) {
		if (values == null || values.isEmpty()) {
			return "";
		}
		return values.stream()
				.map(value -> "\"" + value.name() + "\"")
				.reduce((left, right) -> left + "," + right)
				.orElse("");
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
