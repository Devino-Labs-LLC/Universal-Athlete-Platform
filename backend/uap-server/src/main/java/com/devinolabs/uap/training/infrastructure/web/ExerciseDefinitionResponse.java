package com.devinolabs.uap.training.infrastructure.web;

import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.training.application.ExerciseDefinitionResult;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

/**
 * @param exercisePerformanceKey the same UUID as {@code id}, usable directly on the performance
 * endpoints
 */
record ExerciseDefinitionResponse(
		UUID id,
		UUID exercisePerformanceKey,
		ExerciseDefinitionScope scope,
		String canonicalName,
		String normalizedName,
		ExerciseDefinitionMetadataResponse metadata,
		boolean active,
		Instant archivedAt,
		Instant createdAt,
		Instant updatedAt) {

	static ExerciseDefinitionResponse from(ExerciseDefinitionResult result) {
		return new ExerciseDefinitionResponse(
				result.id().value(),
				result.exercisePerformanceKey().value(),
				result.scope(),
				result.canonicalName(),
				result.normalizedName(),
				ExerciseDefinitionMetadataResponse.from(result.metadata()),
				result.active(),
				result.archivedAt(),
				result.createdAt(),
				result.updatedAt());
	}

}
