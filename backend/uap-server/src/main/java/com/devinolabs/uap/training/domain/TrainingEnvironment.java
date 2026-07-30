package com.devinolabs.uap.training.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

/**
 * Athlete-owned named training context with an explicit set of available equipment.
 *
 * <p>Environment type is descriptive only and never implies equipment. Historical occurrence
 * snapshots retain the name and equipment that were current when the snapshot was taken.
 */
public class TrainingEnvironment {

	private static final int MIN_NAME_LENGTH = 2;
	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_DESCRIPTION_LENGTH = 2000;
	private static final int MAX_FACILITY_NOTES_LENGTH = 2000;

	private final TrainingEnvironmentId id;
	private final AthleteId athleteId;
	private String name;
	private String normalizedName;
	private TrainingEnvironmentType type;
	private List<EquipmentType> availableEquipment;
	private String description;
	private String facilityNotes;
	private boolean defaultEnvironment;
	private boolean active;
	private Instant archivedAt;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private TrainingEnvironment(
			TrainingEnvironmentId id,
			AthleteId athleteId,
			String name,
			String normalizedName,
			TrainingEnvironmentType type,
			List<EquipmentType> availableEquipment,
			String description,
			String facilityNotes,
			boolean defaultEnvironment,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.name = requireName(name);
		this.normalizedName = Objects.requireNonNull(normalizedName, "normalizedName must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.availableEquipment = List.copyOf(availableEquipment);
		this.description = normalizeOptionalText(description, MAX_DESCRIPTION_LENGTH, "description");
		this.facilityNotes = normalizeOptionalText(facilityNotes, MAX_FACILITY_NOTES_LENGTH, "facilityNotes");
		if (!active && defaultEnvironment) {
			throw new TrainingEnvironmentDefaultConflictException(
					"An archived training environment cannot be the default");
		}
		if (!active && archivedAt == null) {
			throw new IllegalArgumentException("archivedAt is required when a training environment is inactive");
		}
		if (active && archivedAt != null) {
			throw new IllegalArgumentException("archivedAt is only allowed when a training environment is inactive");
		}
		this.defaultEnvironment = defaultEnvironment;
		this.active = active;
		this.archivedAt = archivedAt;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static TrainingEnvironment create(
			TrainingEnvironmentId id,
			AthleteId athleteId,
			String name,
			TrainingEnvironmentType type,
			Collection<EquipmentType> availableEquipment,
			String description,
			String facilityNotes,
			boolean defaultEnvironment,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new TrainingEnvironment(
				id,
				athleteId,
				name,
				normalizeName(name),
				type,
				orderedUniqueEquipment(availableEquipment),
				description,
				facilityNotes,
				defaultEnvironment,
				true,
				null,
				now,
				now,
				0L);
	}

	public static TrainingEnvironment rehydrate(
			TrainingEnvironmentId id,
			AthleteId athleteId,
			String name,
			String normalizedName,
			TrainingEnvironmentType type,
			Collection<EquipmentType> availableEquipment,
			String description,
			String facilityNotes,
			boolean defaultEnvironment,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new TrainingEnvironment(
				id,
				athleteId,
				name,
				normalizedName,
				type,
				orderedUniqueEquipment(availableEquipment),
				description,
				facilityNotes,
				defaultEnvironment,
				active,
				archivedAt,
				createdAt,
				updatedAt,
				version);
	}

	public void rename(String name, Clock clock) {
		requireActiveMutable(clock);
		this.name = requireName(name);
		this.normalizedName = normalizeName(name);
		touch(clock);
	}

	public void changeType(TrainingEnvironmentType type, Clock clock) {
		requireActiveMutable(clock);
		this.type = Objects.requireNonNull(type, "type must not be null");
		touch(clock);
	}

	public void replaceAvailableEquipment(Collection<EquipmentType> availableEquipment, Clock clock) {
		requireActiveMutable(clock);
		this.availableEquipment = List.copyOf(orderedUniqueEquipment(availableEquipment));
		touch(clock);
	}

	public void updateDescription(String description, Clock clock) {
		requireActiveMutable(clock);
		this.description = normalizeOptionalText(description, MAX_DESCRIPTION_LENGTH, "description");
		touch(clock);
	}

	public void updateFacilityNotes(String facilityNotes, Clock clock) {
		requireActiveMutable(clock);
		this.facilityNotes = normalizeOptionalText(facilityNotes, MAX_FACILITY_NOTES_LENGTH, "facilityNotes");
		touch(clock);
	}

	public void markDefault(Clock clock) {
		requireActiveMutable(clock);
		this.defaultEnvironment = true;
		touch(clock);
	}

	public void clearDefault(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!defaultEnvironment) {
			return;
		}
		this.defaultEnvironment = false;
		touch(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!active) {
			return;
		}
		this.active = false;
		this.defaultEnvironment = false;
		this.archivedAt = Instant.now(clock);
		touch(clock);
	}

	public boolean isOwnedBy(AthleteId candidate) {
		return athleteId.equals(candidate);
	}

	public static String normalizeName(String name) {
		return ExerciseNameNormalizer.normalize(requireName(name));
	}

	private void requireActiveMutable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!active) {
			throw new TrainingEnvironmentArchivedException();
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new InvalidTrainingEnvironmentNameException("name must not be blank");
		}
		String trimmed = name.trim();
		if (trimmed.length() < MIN_NAME_LENGTH) {
			throw new InvalidTrainingEnvironmentNameException(
					"name must be at least " + MIN_NAME_LENGTH + " characters");
		}
		if (trimmed.length() > MAX_NAME_LENGTH) {
			throw new InvalidTrainingEnvironmentNameException(
					"name must not exceed " + MAX_NAME_LENGTH + " characters");
		}
		return trimmed;
	}

	private static String normalizeOptionalText(String value, int maxLength, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
		}
		return trimmed;
	}

	private static List<EquipmentType> orderedUniqueEquipment(Collection<EquipmentType> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		EnumSet<EquipmentType> seen = EnumSet.noneOf(EquipmentType.class);
		List<EquipmentType> ordered = new ArrayList<>();
		for (EquipmentType value : values) {
			if (value == null) {
				throw new InvalidTrainingEnvironmentEquipmentException(
						"availableEquipment must not contain null");
			}
			if (!seen.add(value)) {
				throw new InvalidTrainingEnvironmentEquipmentException(
						"Duplicate equipment type: " + value);
			}
			ordered.add(value);
		}
		ordered.sort(Comparator.comparingInt(Enum::ordinal));
		return ordered;
	}

	public TrainingEnvironmentId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public String name() {
		return name;
	}

	public String normalizedName() {
		return normalizedName;
	}

	public TrainingEnvironmentType type() {
		return type;
	}

	public List<EquipmentType> availableEquipment() {
		return availableEquipment;
	}

	public String description() {
		return description;
	}

	public String facilityNotes() {
		return facilityNotes;
	}

	public boolean defaultEnvironment() {
		return defaultEnvironment;
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
