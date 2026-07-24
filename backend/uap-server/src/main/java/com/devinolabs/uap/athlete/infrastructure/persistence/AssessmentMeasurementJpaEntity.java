package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

@Entity
@Table(name = "assessment_measurements")
class AssessmentMeasurementJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "assessment_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID assessmentId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "source_measurement_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID sourceMeasurementId;

	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	@Column(name = "label", length = 160)
	private String label;

	@Column(name = "notes", length = 1000)
	private String notes;

	@Enumerated(EnumType.STRING)
	@Column(name = "snapshot_measurement_type", length = 60)
	private MeasurementType snapshotMeasurementType;

	@Column(name = "snapshot_custom_measurement_name", length = 120)
	private String snapshotCustomMeasurementName;

	@Column(name = "snapshot_value", precision = 14, scale = 4)
	private BigDecimal snapshotValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "snapshot_unit", length = 60)
	private MeasurementUnit snapshotUnit;

	@Column(name = "snapshot_custom_unit", length = 60)
	private String snapshotCustomUnit;

	@Enumerated(EnumType.STRING)
	@Column(name = "snapshot_source", length = 30)
	private MeasurementSource snapshotSource;

	@Column(name = "snapshot_measured_at")
	private Instant snapshotMeasuredAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "snapshot_athlete_sport_id", columnDefinition = "BINARY(16)")
	private UUID snapshotAthleteSportId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "snapshot_athlete_goal_id", columnDefinition = "BINARY(16)")
	private UUID snapshotAthleteGoalId;

	@Column(name = "snapshotted_at")
	private Instant snapshottedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AssessmentMeasurementJpaEntity() {
	}

	AssessmentMeasurementJpaEntity(
			UUID id,
			UUID assessmentId,
			UUID athleteId,
			UUID sourceMeasurementId,
			int displayOrder,
			String label,
			String notes,
			MeasurementType snapshotMeasurementType,
			String snapshotCustomMeasurementName,
			BigDecimal snapshotValue,
			MeasurementUnit snapshotUnit,
			String snapshotCustomUnit,
			MeasurementSource snapshotSource,
			Instant snapshotMeasuredAt,
			UUID snapshotAthleteSportId,
			UUID snapshotAthleteGoalId,
			Instant snapshottedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.assessmentId = assessmentId;
		this.athleteId = athleteId;
		this.sourceMeasurementId = sourceMeasurementId;
		this.displayOrder = displayOrder;
		this.label = label;
		this.notes = notes;
		this.snapshotMeasurementType = snapshotMeasurementType;
		this.snapshotCustomMeasurementName = snapshotCustomMeasurementName;
		this.snapshotValue = snapshotValue;
		this.snapshotUnit = snapshotUnit;
		this.snapshotCustomUnit = snapshotCustomUnit;
		this.snapshotSource = snapshotSource;
		this.snapshotMeasuredAt = snapshotMeasuredAt;
		this.snapshotAthleteSportId = snapshotAthleteSportId;
		this.snapshotAthleteGoalId = snapshotAthleteGoalId;
		this.snapshottedAt = snapshottedAt;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	UUID getAssessmentId() {
		return assessmentId;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	UUID getSourceMeasurementId() {
		return sourceMeasurementId;
	}

	int getDisplayOrder() {
		return displayOrder;
	}

	String getLabel() {
		return label;
	}

	String getNotes() {
		return notes;
	}

	MeasurementType getSnapshotMeasurementType() {
		return snapshotMeasurementType;
	}

	String getSnapshotCustomMeasurementName() {
		return snapshotCustomMeasurementName;
	}

	BigDecimal getSnapshotValue() {
		return snapshotValue;
	}

	MeasurementUnit getSnapshotUnit() {
		return snapshotUnit;
	}

	String getSnapshotCustomUnit() {
		return snapshotCustomUnit;
	}

	MeasurementSource getSnapshotSource() {
		return snapshotSource;
	}

	Instant getSnapshotMeasuredAt() {
		return snapshotMeasuredAt;
	}

	UUID getSnapshotAthleteSportId() {
		return snapshotAthleteSportId;
	}

	UUID getSnapshotAthleteGoalId() {
		return snapshotAthleteGoalId;
	}

	Instant getSnapshottedAt() {
		return snapshottedAt;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

}
