package com.devinolabs.uap.training.infrastructure.persistence;

import java.io.Serializable;
import java.time.LocalDate;
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

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;
import com.devinolabs.uap.training.infrastructure.persistence.DailyAthleteStateScheduledOccurrenceJpaEntity.Pk;

@Entity
@Table(name = "daily_athlete_state_scheduled_occurrences")
@IdClass(Pk.class)
class DailyAthleteStateScheduledOccurrenceJpaEntity {

	@Id
	@Column(name = "snapshot_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID snapshotId;

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "occurrence_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID occurrenceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
	private DailyAthleteStateSnapshotJpaEntity snapshot;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@Enumerated(EnumType.STRING)
	@Column(name = "occurrence_status", nullable = false, length = 32)
	private WorkoutOccurrenceStatus occurrenceStatus;

	@Column(name = "scheduled_date", nullable = false)
	private LocalDate scheduledDate;

	@Column(name = "planned_environment_name_snapshot", length = 100)
	private String plannedEnvironmentNameSnapshot;

	@Column(name = "actual_environment_name_snapshot", length = 100)
	private String actualEnvironmentNameSnapshot;

	@Column(name = "order_index", nullable = false)
	private int orderIndex;

	protected DailyAthleteStateScheduledOccurrenceJpaEntity() {
	}

	static DailyAthleteStateScheduledOccurrenceJpaEntity of(
			DailyAthleteStateSnapshotJpaEntity snapshot,
			UUID occurrenceId,
			UUID trainingPlanId,
			UUID workoutDayId,
			WorkoutOccurrenceStatus occurrenceStatus,
			LocalDate scheduledDate,
			String plannedEnvironmentNameSnapshot,
			String actualEnvironmentNameSnapshot,
			int orderIndex) {
		DailyAthleteStateScheduledOccurrenceJpaEntity entity = new DailyAthleteStateScheduledOccurrenceJpaEntity();
		entity.snapshot = snapshot;
		entity.snapshotId = snapshot.getId();
		entity.occurrenceId = occurrenceId;
		entity.trainingPlanId = trainingPlanId;
		entity.workoutDayId = workoutDayId;
		entity.occurrenceStatus = occurrenceStatus;
		entity.scheduledDate = scheduledDate;
		entity.plannedEnvironmentNameSnapshot = plannedEnvironmentNameSnapshot;
		entity.actualEnvironmentNameSnapshot = actualEnvironmentNameSnapshot;
		entity.orderIndex = orderIndex;
		return entity;
	}

	UUID getOccurrenceId() { return occurrenceId; }
	UUID getTrainingPlanId() { return trainingPlanId; }
	UUID getWorkoutDayId() { return workoutDayId; }
	WorkoutOccurrenceStatus getOccurrenceStatus() { return occurrenceStatus; }
	LocalDate getScheduledDate() { return scheduledDate; }
	String getPlannedEnvironmentNameSnapshot() { return plannedEnvironmentNameSnapshot; }
	String getActualEnvironmentNameSnapshot() { return actualEnvironmentNameSnapshot; }
	int getOrderIndex() { return orderIndex; }

	public static final class Pk implements Serializable {
		private UUID snapshotId;
		private UUID occurrenceId;

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
			return Objects.equals(snapshotId, that.snapshotId) && Objects.equals(occurrenceId, that.occurrenceId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(snapshotId, occurrenceId);
		}
	}

}
