package com.devinolabs.uap.athlete.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

public class AssessmentMeasurement {

	private static final int MAX_LABEL_LENGTH = 160;
	private static final int MAX_NOTES_LENGTH = 1000;

	private final AssessmentMeasurementId id;
	private final AssessmentId assessmentId;
	private final AthleteId athleteId;
	private final AthleteMeasurementId sourceMeasurementId;
	private int displayOrder;
	private String label;
	private String notes;
	private AssessmentMeasurementSnapshot snapshot;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private AssessmentMeasurement(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId,
			AthleteMeasurementId sourceMeasurementId,
			int displayOrder,
			String label,
			String notes,
			AssessmentMeasurementSnapshot snapshot,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.assessmentId = Objects.requireNonNull(assessmentId, "assessmentId must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.sourceMeasurementId = Objects.requireNonNull(sourceMeasurementId, "sourceMeasurementId must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		this.label = normalizeOptionalText(label, MAX_LABEL_LENGTH, "label");
		this.notes = normalizeOptionalText(notes, MAX_NOTES_LENGTH, "notes");
		this.snapshot = snapshot;
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static AssessmentMeasurement attach(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId,
			AthleteMeasurementId sourceMeasurementId,
			int displayOrder,
			String label,
			String notes,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new AssessmentMeasurement(
				id,
				assessmentId,
				athleteId,
				sourceMeasurementId,
				displayOrder,
				label,
				notes,
				null,
				now,
				now,
				0L);
	}

	public static AssessmentMeasurement rehydrate(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId,
			AthleteMeasurementId sourceMeasurementId,
			int displayOrder,
			String label,
			String notes,
			AssessmentMeasurementSnapshot snapshot,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new AssessmentMeasurement(
				id,
				assessmentId,
				athleteId,
				sourceMeasurementId,
				displayOrder,
				label,
				notes,
				snapshot,
				createdAt,
				updatedAt,
				version);
	}

	public void changeDisplayOrder(int displayOrder, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.displayOrder = requireDisplayOrder(displayOrder);
		touch(clock);
	}

	public void changeLabel(String label, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.label = normalizeOptionalText(label, MAX_LABEL_LENGTH, "label");
		touch(clock);
	}

	public void changeNotes(String notes, Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		this.notes = normalizeOptionalText(notes, MAX_NOTES_LENGTH, "notes");
		touch(clock);
	}

	public void captureSnapshot(AthleteMeasurement source, Clock clock) {
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(clock, "Clock must not be null");
		if (!source.id().equals(sourceMeasurementId)) {
			throw new IllegalArgumentException("Snapshot source must match attached sourceMeasurementId");
		}
		if (!source.athleteId().equals(athleteId)) {
			throw new IllegalArgumentException("Snapshot source must belong to the same athlete");
		}
		Instant now = Instant.now(clock);
		this.snapshot = AssessmentMeasurementSnapshot.from(source, now);
		this.updatedAt = now;
	}

	public boolean isSnapshotted() {
		return snapshot != null;
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static int requireDisplayOrder(int displayOrder) {
		if (displayOrder < 0) {
			throw new IllegalArgumentException("displayOrder must not be negative");
		}
		return displayOrder;
	}

	private static String normalizeOptionalText(String value, int maxLength, String fieldName) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.length() > maxLength) {
			throw new IllegalArgumentException(fieldName + " must not exceed " + maxLength + " characters");
		}
		return normalized;
	}

	public AssessmentMeasurementId id() {
		return id;
	}

	public AssessmentId assessmentId() {
		return assessmentId;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public AthleteMeasurementId sourceMeasurementId() {
		return sourceMeasurementId;
	}

	public int displayOrder() {
		return displayOrder;
	}

	public String label() {
		return label;
	}

	public String notes() {
		return notes;
	}

	public AssessmentMeasurementSnapshot snapshot() {
		return snapshot;
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
