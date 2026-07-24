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
@Table(name = "athlete_measurements")
class AthleteMeasurementJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Enumerated(EnumType.STRING)
	@Column(name = "measurement_type", nullable = false, length = 60, updatable = false)
	private MeasurementType measurementType;

	@Column(name = "custom_measurement_name", length = 120, updatable = false)
	private String customMeasurementName;

	@Column(name = "measurement_value", nullable = false, precision = 14, scale = 4)
	private BigDecimal measurementValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "measurement_unit", nullable = false, length = 60)
	private MeasurementUnit measurementUnit;

	@Column(name = "custom_unit", length = 60)
	private String customUnit;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false, length = 30, updatable = false)
	private MeasurementSource source;

	@Column(name = "notes", length = 1000)
	private String notes;

	@Column(name = "measured_at", nullable = false)
	private Instant measuredAt;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_sport_id", columnDefinition = "BINARY(16)")
	private UUID athleteSportId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_goal_id", columnDefinition = "BINARY(16)")
	private UUID athleteGoalId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AthleteMeasurementJpaEntity() {
	}

	AthleteMeasurementJpaEntity(
			UUID id,
			UUID athleteId,
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal measurementValue,
			MeasurementUnit measurementUnit,
			String customUnit,
			MeasurementSource source,
			String notes,
			Instant measuredAt,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.athleteId = athleteId;
		this.measurementType = measurementType;
		this.customMeasurementName = customMeasurementName;
		this.measurementValue = measurementValue;
		this.measurementUnit = measurementUnit;
		this.customUnit = customUnit;
		this.source = source;
		this.notes = notes;
		this.measuredAt = measuredAt;
		this.athleteSportId = athleteSportId;
		this.athleteGoalId = athleteGoalId;
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

	UUID getAthleteId() {
		return athleteId;
	}

	MeasurementType getMeasurementType() {
		return measurementType;
	}

	String getCustomMeasurementName() {
		return customMeasurementName;
	}

	BigDecimal getMeasurementValue() {
		return measurementValue;
	}

	MeasurementUnit getMeasurementUnit() {
		return measurementUnit;
	}

	String getCustomUnit() {
		return customUnit;
	}

	MeasurementSource getSource() {
		return source;
	}

	String getNotes() {
		return notes;
	}

	Instant getMeasuredAt() {
		return measuredAt;
	}

	UUID getAthleteSportId() {
		return athleteSportId;
	}

	UUID getAthleteGoalId() {
		return athleteGoalId;
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
