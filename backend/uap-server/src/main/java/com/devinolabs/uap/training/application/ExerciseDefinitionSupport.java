package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseNameNormalizer;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitionModificationNotAllowedException;

final class ExerciseDefinitionSupport {

	static final int DEFAULT_PAGE_SIZE = 20;

	static final int MAX_PAGE_SIZE = 100;

	private static final int MAX_NAME_FILTER_LENGTH = 150;

	private ExerciseDefinitionSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	/**
	 * Loads a definition the athlete may see, hiding another athlete's custom exercises behind the
	 * same response as a missing row.
	 */
	static ExerciseDefinition requireAccessible(
			ExerciseDefinitionRepository repository,
			AthleteId athleteId,
			ExerciseDefinitionId definitionId) {
		ExerciseDefinition definition = repository
				.findById(definitionId)
				.orElseThrow(ExerciseDefinitionNotFoundException::new);
		if (!ExerciseDefinitionAccessPolicy.isAccessible(athleteId, definition)) {
			throw new ExerciseDefinitionNotAccessibleException();
		}
		return definition;
	}

	/**
	 * Loads one of the athlete's own custom definitions for modification.
	 */
	static ExerciseDefinition requireOwnCustom(
			ExerciseDefinitionRepository repository,
			AthleteId athleteId,
			ExerciseDefinitionId definitionId) {
		ExerciseDefinition definition = requireAccessible(repository, athleteId, definitionId);
		if (definition.scope() != ExerciseDefinitionScope.ATHLETE_CUSTOM) {
			throw new SystemExerciseDefinitionModificationNotAllowedException();
		}
		return definition;
	}

	static void assertNoActiveDuplicate(
			ExerciseDefinitionRepository repository,
			AthleteId athleteId,
			String canonicalName,
			ExerciseDefinitionId excludingId) {
		String normalized = ExerciseDefinition.normalizeName(canonicalName);
		boolean exists = excludingId == null
				? repository.existsActiveCustomByAthleteIdAndNormalizedName(athleteId, normalized)
				: repository.existsActiveCustomByAthleteIdAndNormalizedNameExcluding(
						athleteId, normalized, excludingId);
		if (exists) {
			throw new DuplicateExerciseDefinitionException();
		}
	}

	static int requirePage(Integer page) {
		int resolved = page == null ? 0 : page;
		if (resolved < 0) {
			throw new InvalidExerciseDefinitionQueryException("page must be >= 0");
		}
		return resolved;
	}

	static int requireSize(Integer size) {
		int resolved = size == null ? DEFAULT_PAGE_SIZE : size;
		if (resolved < 1 || resolved > MAX_PAGE_SIZE) {
			throw new InvalidExerciseDefinitionQueryException("size must be between 1 and " + MAX_PAGE_SIZE);
		}
		return resolved;
	}

	static String normalizeNameFilter(String nameContains) {
		if (nameContains == null || nameContains.isBlank()) {
			return null;
		}
		String trimmed = nameContains.trim();
		if (trimmed.length() > MAX_NAME_FILTER_LENGTH) {
			throw new InvalidExerciseDefinitionQueryException(
					"name must not exceed " + MAX_NAME_FILTER_LENGTH + " characters");
		}
		return ExerciseNameNormalizer.normalize(trimmed);
	}

	static ExerciseDefinitionResult toResult(ExerciseDefinition definition) {
		Objects.requireNonNull(definition, "definition must not be null");
		return new ExerciseDefinitionResult(
				definition.id(),
				definition.performanceKey(),
				definition.scope(),
				definition.canonicalName(),
				definition.normalizedName(),
				definition.active(),
				definition.archivedAt(),
				definition.createdAt(),
				definition.updatedAt());
	}

	static List<ExerciseDefinitionResult> toResults(List<ExerciseDefinition> definitions) {
		return definitions.stream().map(ExerciseDefinitionSupport::toResult).toList();
	}

	static ExerciseDefinitionPageResult toPageResult(ExerciseDefinitionPage page) {
		return new ExerciseDefinitionPageResult(
				toResults(page.definitions()),
				page.page(),
				page.size(),
				page.totalElements(),
				page.totalPages());
	}

}
