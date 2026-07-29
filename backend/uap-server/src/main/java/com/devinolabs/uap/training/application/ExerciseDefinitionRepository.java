package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

public interface ExerciseDefinitionRepository {

	ExerciseDefinition save(ExerciseDefinition definition);

	Optional<ExerciseDefinition> findById(ExerciseDefinitionId id);

	/**
	 * Resolves a definition the athlete is allowed to see: any SYSTEM definition or one of their
	 * own custom definitions, active or archived.
	 */
	Optional<ExerciseDefinition> findAccessible(ExerciseDefinitionId id, AthleteId athleteId);

	boolean existsActiveSystemByNormalizedName(String normalizedName);

	boolean existsActiveCustomByAthleteIdAndNormalizedName(AthleteId athleteId, String normalizedName);

	boolean existsActiveCustomByAthleteIdAndNormalizedNameExcluding(
			AthleteId athleteId,
			String normalizedName,
			ExerciseDefinitionId excludingId);

	/**
	 * Active SYSTEM definitions plus the athlete's own active custom definitions, ordered by
	 * canonical name then id, optionally narrowed by scope, name fragment, and catalogue metadata.
	 */
	ExerciseDefinitionPage findAccessibleActive(AthleteId athleteId, ExerciseDefinitionFilters filters, int page, int size);

	/**
	 * Loads several definitions by id in one round trip for candidate ordering.
	 */
	List<ExerciseDefinition> findAllByIds(List<ExerciseDefinitionId> ids);

}
