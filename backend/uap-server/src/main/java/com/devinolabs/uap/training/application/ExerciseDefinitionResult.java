package com.devinolabs.uap.training.application;

import java.time.Instant;

import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;

/**
 * @param exercisePerformanceKey the same UUID as {@code id}; exposed so clients can move between
 *                               definition endpoints and performance endpoints without a lookup
 */
public record ExerciseDefinitionResult(
		ExerciseDefinitionId id,
		ExercisePerformanceKey exercisePerformanceKey,
		ExerciseDefinitionScope scope,
		String canonicalName,
		String normalizedName,
		ExerciseDefinitionMetadataResult metadata,
		boolean active,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {
}
