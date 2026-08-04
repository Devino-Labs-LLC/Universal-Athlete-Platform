package com.devinolabs.uap.training.infrastructure.persistence;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.infrastructure.persistence.DailyAthleteStateCategorySummaryJpaEntity.Pk;

@Entity
@Table(name = "daily_athlete_state_category_summaries")
@IdClass(Pk.class)
class DailyAthleteStateCategorySummaryJpaEntity {

	@Id
	@Column(name = "snapshot_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID snapshotId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32)
	private ExerciseDefinitionCategory category;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
	private DailyAthleteStateSnapshotJpaEntity snapshot;

	@Column(name = "completed_exercise_count", nullable = false)
	private long completedExerciseCount;

	@Column(name = "completed_set_count", nullable = false)
	private long completedSetCount;

	@Column(name = "volume_kilograms", nullable = false, precision = 18, scale = 3)
	private BigDecimal volumeKilograms;

	@Column(name = "duration_seconds", nullable = false)
	private long durationSeconds;

	@Column(name = "distance_meters", nullable = false, precision = 18, scale = 3)
	private BigDecimal distanceMeters;

	protected DailyAthleteStateCategorySummaryJpaEntity() {
	}

	static DailyAthleteStateCategorySummaryJpaEntity of(
			DailyAthleteStateSnapshotJpaEntity snapshot,
			ExerciseDefinitionCategory category,
			long completedExerciseCount,
			long completedSetCount,
			BigDecimal volumeKilograms,
			long durationSeconds,
			BigDecimal distanceMeters) {
		DailyAthleteStateCategorySummaryJpaEntity entity = new DailyAthleteStateCategorySummaryJpaEntity();
		entity.snapshot = snapshot;
		entity.snapshotId = snapshot.getId();
		entity.category = category;
		entity.completedExerciseCount = completedExerciseCount;
		entity.completedSetCount = completedSetCount;
		entity.volumeKilograms = volumeKilograms;
		entity.durationSeconds = durationSeconds;
		entity.distanceMeters = distanceMeters;
		return entity;
	}

	ExerciseDefinitionCategory getCategory() { return category; }
	long getCompletedExerciseCount() { return completedExerciseCount; }
	long getCompletedSetCount() { return completedSetCount; }
	BigDecimal getVolumeKilograms() { return volumeKilograms; }
	long getDurationSeconds() { return durationSeconds; }
	BigDecimal getDistanceMeters() { return distanceMeters; }

	public static final class Pk implements Serializable {
		private UUID snapshotId;
		private ExerciseDefinitionCategory category;

		public Pk() {
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof Pk that)) {
				return false;
			}
			return Objects.equals(snapshotId, that.snapshotId) && category == that.category;
		}

		@Override
		public int hashCode() {
			return Objects.hash(snapshotId, category);
		}
	}

}
