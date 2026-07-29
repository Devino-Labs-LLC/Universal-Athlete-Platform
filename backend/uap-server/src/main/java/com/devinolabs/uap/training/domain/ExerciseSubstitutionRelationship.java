package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/**
 * Explicit directed catalogue relationship: for a prescribed source movement, the target may be
 * considered an approved substitute under the recorded relationship semantics.
 *
 * <p>Direction matters. Source→target does not imply the reverse. SYSTEM-to-SYSTEM rows are seeded;
 * athletes own only relationships they create.
 */
public class ExerciseSubstitutionRelationship {

	private static final int MAX_RATIONALE_LENGTH = 2000;

	private final ExerciseSubstitutionRelationshipId id;
	private final AthleteId ownerAthleteId;
	private final ExerciseDefinitionId sourceExerciseDefinitionId;
	private final ExerciseDefinitionId targetExerciseDefinitionId;
	private ExerciseSubstitutionRelationshipType relationshipType;
	private ExerciseSubstitutionCompatibility compatibilityLevel;
	private String rationale;
	private boolean active;
	private Instant archivedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private ExerciseSubstitutionRelationship(
			ExerciseSubstitutionRelationshipId id,
			AthleteId ownerAthleteId,
			ExerciseDefinitionId sourceExerciseDefinitionId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.ownerAthleteId = ownerAthleteId;
		this.sourceExerciseDefinitionId = Objects.requireNonNull(
				sourceExerciseDefinitionId, "sourceExerciseDefinitionId must not be null");
		this.targetExerciseDefinitionId = Objects.requireNonNull(
				targetExerciseDefinitionId, "targetExerciseDefinitionId must not be null");
		if (sourceExerciseDefinitionId.equals(targetExerciseDefinitionId)) {
			throw new InvalidExerciseSubstitutionRelationshipException(
					"A substitution relationship source cannot equal its target");
		}
		this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType must not be null");
		this.compatibilityLevel = Objects.requireNonNull(
				compatibilityLevel, "compatibilityLevel must not be null");
		this.rationale = normalizeRationale(rationale);
		if (!active && archivedAt == null) {
			throw new InvalidExerciseSubstitutionRelationshipException(
					"archivedAt is required when a relationship is inactive");
		}
		if (active && archivedAt != null) {
			throw new InvalidExerciseSubstitutionRelationshipException(
					"archivedAt is only allowed when a relationship is inactive");
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

	public static ExerciseSubstitutionRelationship createOwned(
			ExerciseSubstitutionRelationshipId id,
			AthleteId ownerAthleteId,
			ExerciseDefinitionId sourceExerciseDefinitionId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			Clock clock) {
		Objects.requireNonNull(ownerAthleteId, "ownerAthleteId must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new ExerciseSubstitutionRelationship(
				id,
				ownerAthleteId,
				sourceExerciseDefinitionId,
				targetExerciseDefinitionId,
				relationshipType,
				compatibilityLevel,
				rationale,
				true,
				null,
				now,
				now,
				0L);
	}

	public static ExerciseSubstitutionRelationship createSystem(
			ExerciseSubstitutionRelationshipId id,
			ExerciseDefinitionId sourceExerciseDefinitionId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new ExerciseSubstitutionRelationship(
				id,
				null,
				sourceExerciseDefinitionId,
				targetExerciseDefinitionId,
				relationshipType,
				compatibilityLevel,
				rationale,
				true,
				null,
				now,
				now,
				0L);
	}

	public static ExerciseSubstitutionRelationship rehydrate(
			ExerciseSubstitutionRelationshipId id,
			AthleteId ownerAthleteId,
			ExerciseDefinitionId sourceExerciseDefinitionId,
			ExerciseDefinitionId targetExerciseDefinitionId,
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new ExerciseSubstitutionRelationship(
				id,
				ownerAthleteId,
				sourceExerciseDefinitionId,
				targetExerciseDefinitionId,
				relationshipType,
				compatibilityLevel,
				rationale,
				active,
				archivedAt,
				createdAt,
				updatedAt,
				version);
	}

	public void update(
			ExerciseSubstitutionRelationshipType relationshipType,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			String rationale,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireOwnedMutable();
		this.relationshipType = Objects.requireNonNull(relationshipType, "relationshipType must not be null");
		this.compatibilityLevel = Objects.requireNonNull(
				compatibilityLevel, "compatibilityLevel must not be null");
		this.rationale = normalizeRationale(rationale);
		touch(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		requireOwnedMutable();
		if (!active) {
			return;
		}
		this.active = false;
		this.archivedAt = Instant.now(clock);
		touch(clock);
	}

	public boolean isSystemOwned() {
		return ownerAthleteId == null;
	}

	public boolean isOwnedBy(AthleteId athleteId) {
		return ownerAthleteId != null && ownerAthleteId.equals(athleteId);
	}

	private void requireOwnedMutable() {
		if (isSystemOwned()) {
			throw new SystemExerciseSubstitutionRelationshipModificationNotAllowedException();
		}
		if (!active) {
			throw new ExerciseSubstitutionRelationshipArchivedException();
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	static String normalizeRationale(String rationale) {
		if (rationale == null || rationale.isBlank()) {
			return null;
		}
		String trimmed = rationale.trim();
		if (trimmed.length() > MAX_RATIONALE_LENGTH) {
			throw new InvalidExerciseSubstitutionRelationshipException(
					"rationale must not exceed " + MAX_RATIONALE_LENGTH + " characters");
		}
		return trimmed;
	}

	public ExerciseSubstitutionRelationshipId id() {
		return id;
	}

	public AthleteId ownerAthleteId() {
		return ownerAthleteId;
	}

	public ExerciseDefinitionId sourceExerciseDefinitionId() {
		return sourceExerciseDefinitionId;
	}

	public ExerciseDefinitionId targetExerciseDefinitionId() {
		return targetExerciseDefinitionId;
	}

	public ExerciseSubstitutionRelationshipType relationshipType() {
		return relationshipType;
	}

	public ExerciseSubstitutionCompatibility compatibilityLevel() {
		return compatibilityLevel;
	}

	public String rationale() {
		return rationale;
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
