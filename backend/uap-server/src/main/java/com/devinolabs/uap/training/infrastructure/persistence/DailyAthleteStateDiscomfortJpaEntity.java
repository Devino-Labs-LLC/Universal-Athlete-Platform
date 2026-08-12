package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;

@Entity
@Table(name = "daily_athlete_state_discomfort")
class DailyAthleteStateDiscomfortJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", nullable = false)
	private DailyAthleteStateSnapshotJpaEntity snapshot;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_area", nullable = false, length = 40)
	private BodyArea bodyArea;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_side", nullable = false, length = 32)
	private BodySide bodySide;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "intensity", nullable = false)
	private int intensity;

	@Column(name = "notes", length = 250)
	private String notes;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	protected DailyAthleteStateDiscomfortJpaEntity() {
	}

	static DailyAthleteStateDiscomfortJpaEntity of(
			UUID id,
			DailyAthleteStateSnapshotJpaEntity snapshot,
			BodyArea bodyArea,
			BodySide bodySide,
			int intensity,
			String notes,
			int orderIndex) {
		DailyAthleteStateDiscomfortJpaEntity entity = new DailyAthleteStateDiscomfortJpaEntity();
		entity.id = id;
		entity.snapshot = snapshot;
		entity.bodyArea = bodyArea;
		entity.bodySide = bodySide;
		entity.intensity = intensity;
		entity.notes = notes;
		entity.orderIndex = orderIndex;
		return entity;
	}

	UUID getId() { return id; }
	BodyArea getBodyArea() { return bodyArea; }
	BodySide getBodySide() { return bodySide; }
	int getIntensity() { return intensity; }
	String getNotes() { return notes; }
	int getOrderIndex() { return orderIndex; }

}
