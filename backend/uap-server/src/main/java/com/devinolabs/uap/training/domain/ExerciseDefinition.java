package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * The canonical identity of a movement, independent of any plan that prescribes it.
 *
 * <p>An athlete's performance history and personal records are aggregated under this id, so the id
 * must survive renames and must be shared by every prescription of the same movement across every
 * plan. Catalogue metadata (category, equipment, muscles, …) describes the movement factually and
 * never rewrites historical executions or personal-record grouping.
 */
public class ExerciseDefinition {

	private static final int MIN_NAME_LENGTH = 2;
	private static final int MAX_NAME_LENGTH = 150;

	private final ExerciseDefinitionId id;
	private final ExerciseDefinitionScope scope;
	private final AthleteId athleteId;
	private String canonicalName;
	private String normalizedName;
	private ExerciseDefinitionMetadata metadata;
	private boolean active;
	private Instant archivedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private ExerciseDefinition(
			ExerciseDefinitionId id,
			ExerciseDefinitionScope scope,
			AthleteId athleteId,
			String canonicalName,
			String normalizedName,
			ExerciseDefinitionMetadata metadata,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.scope = Objects.requireNonNull(scope, "scope must not be null");
		if (scope == ExerciseDefinitionScope.SYSTEM && athleteId != null) {
			throw new IllegalArgumentException("SYSTEM exercise definitions must not be athlete owned");
		}
		if (scope == ExerciseDefinitionScope.ATHLETE_CUSTOM && athleteId == null) {
			throw new IllegalArgumentException("ATHLETE_CUSTOM exercise definitions require an athleteId");
		}
		this.athleteId = athleteId;
		this.canonicalName = requireCanonicalName(canonicalName);
		this.normalizedName = Objects.requireNonNull(normalizedName, "normalizedName must not be null");
		this.metadata = Objects.requireNonNull(metadata, "metadata must not be null");
		if (!active && archivedAt == null) {
			throw new IllegalArgumentException("archivedAt is required when an exercise definition is inactive");
		}
		if (active && archivedAt != null) {
			throw new IllegalArgumentException("archivedAt is only allowed when an exercise definition is inactive");
		}
		this.active = active;
		this.archivedAt = archivedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static ExerciseDefinition createSystem(
			ExerciseDefinitionId id,
			String canonicalName,
			ExerciseDefinitionMetadata metadata,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new ExerciseDefinition(
				id,
				ExerciseDefinitionScope.SYSTEM,
				null,
				canonicalName,
				normalizeName(canonicalName),
				metadata,
				true,
				null,
				now,
				now,
				0L);
	}

	public static ExerciseDefinition createAthleteCustom(
			ExerciseDefinitionId id,
			AthleteId athleteId,
			String canonicalName,
			ExerciseDefinitionMetadata metadata,
			Clock clock) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new ExerciseDefinition(
				id,
				ExerciseDefinitionScope.ATHLETE_CUSTOM,
				athleteId,
				canonicalName,
				normalizeName(canonicalName),
				metadata,
				true,
				null,
				now,
				now,
				0L);
	}

	public static ExerciseDefinition rehydrate(
			ExerciseDefinitionId id,
			ExerciseDefinitionScope scope,
			AthleteId athleteId,
			String canonicalName,
			String normalizedName,
			ExerciseDefinitionMetadata metadata,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new ExerciseDefinition(
				id,
				scope,
				athleteId,
				canonicalName,
				normalizedName,
				metadata,
				active,
				archivedAt,
				createdAt,
				updatedAt,
				version);
	}

	/**
	 * Changes the display name while keeping the identity, so existing history stays attached.
	 */
	public void rename(String canonicalName, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireCustom();
		this.canonicalName = requireCanonicalName(canonicalName);
		this.normalizedName = normalizeName(canonicalName);
		touch(clock);
	}

	/**
	 * Replaces catalogue metadata without changing identity or historical performance grouping.
	 */
	public void updateMetadata(ExerciseDefinitionMetadata metadata, Clock clock) {
		Objects.requireNonNull(metadata, "metadata must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		requireCustom();
		this.metadata = metadata;
		touch(clock);
	}

	/**
	 * Retires the definition from selection. History and personal records already recorded under it
	 * are untouched, which is why archiving is offered instead of deletion.
	 */
	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireCustom();
		if (!active) {
			return;
		}
		this.active = false;
		this.archivedAt = Instant.now(clock);
		touch(clock);
	}

	public ExercisePerformanceKey performanceKey() {
		return ExercisePerformanceKey.of(id);
	}

	public boolean isOwnedBy(AthleteId candidate) {
		return athleteId != null && athleteId.equals(candidate);
	}

	public static String normalizeName(String canonicalName) {
		return ExerciseNameNormalizer.normalize(requireCanonicalName(canonicalName));
	}

	private void requireCustom() {
		if (scope != ExerciseDefinitionScope.ATHLETE_CUSTOM) {
			throw new SystemExerciseDefinitionModificationNotAllowedException();
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static String requireCanonicalName(String canonicalName) {
		if (canonicalName == null || canonicalName.isBlank()) {
			throw new InvalidExerciseDefinitionNameException("canonicalName must not be blank");
		}
		String trimmed = canonicalName.trim();
		if (trimmed.length() < MIN_NAME_LENGTH) {
			throw new InvalidExerciseDefinitionNameException(
					"canonicalName must be at least " + MIN_NAME_LENGTH + " characters");
		}
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidExerciseDefinitionNameException(
					"canonicalName must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	public ExerciseDefinitionId id() {
		return id;
	}

	public ExerciseDefinitionScope scope() {
		return scope;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public String canonicalName() {
		return canonicalName;
	}

	public String normalizedName() {
		return normalizedName;
	}

	public ExerciseDefinitionMetadata metadata() {
		return metadata;
	}

	public boolean active() {
		return active;
	}

	public Instant archivedAt() {
		return archivedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
