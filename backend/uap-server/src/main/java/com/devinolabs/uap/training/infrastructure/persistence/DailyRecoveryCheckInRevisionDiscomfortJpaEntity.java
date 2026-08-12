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
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.DiscomfortSnapshotSide;

@Entity
@Table(name = "daily_recovery_check_in_revision_discomfort")
class DailyRecoveryCheckInRevisionDiscomfortJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "revision_id", nullable = false)
	private DailyRecoveryCheckInRevisionJpaEntity revision;

	@Enumerated(EnumType.STRING)
	@Column(name = "snapshot_side", nullable = false, updatable = false, length = 8)
	private DiscomfortSnapshotSide snapshotSide;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_area", nullable = false, updatable = false, length = 40)
	private BodyArea bodyArea;

	@Enumerated(EnumType.STRING)
	@Column(name = "body_side", nullable = false, updatable = false, length = 32)
	private BodySide bodySide;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "intensity", nullable = false, updatable = false)
	private int intensity;

	@Column(name = "notes", length = 250, updatable = false)
	private String notes;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	@Transient
	private boolean isNew = true;

	protected DailyRecoveryCheckInRevisionDiscomfortJpaEntity() {
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

	DiscomfortSnapshotSide getSnapshotSide() {
		return snapshotSide;
	}

	BodyArea getBodyArea() {
		return bodyArea;
	}

	BodySide getBodySide() {
		return bodySide;
	}

	int getIntensity() {
		return intensity;
	}

	String getNotes() {
		return notes;
	}

	int getOrderIndex() {
		return orderIndex;
	}

	void setId(UUID id) {
		this.id = id;
	}

	void setRevision(DailyRecoveryCheckInRevisionJpaEntity revision) {
		this.revision = revision;
	}

	void setSnapshotSide(DiscomfortSnapshotSide snapshotSide) {
		this.snapshotSide = snapshotSide;
	}

	void setBodyArea(BodyArea bodyArea) {
		this.bodyArea = bodyArea;
	}

	void setBodySide(BodySide bodySide) {
		this.bodySide = bodySide;
	}

	void setIntensity(int intensity) {
		this.intensity = intensity;
	}

	void setNotes(String notes) {
		this.notes = notes;
	}

	void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}

	void setNew(boolean isNew) {
		this.isNew = isNew;
	}

}
