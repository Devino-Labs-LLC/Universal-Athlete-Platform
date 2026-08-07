package com.devinolabs.uap.training.infrastructure.persistence;

import java.io.Serializable;
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
import com.devinolabs.uap.training.infrastructure.persistence.DailyTrainingRecommendationOccurrenceJpaEntity.Pk;

@Entity
@Table(name = "daily_training_recommendation_occurrences")
@IdClass(Pk.class)
class DailyTrainingRecommendationOccurrenceJpaEntity {

	@Id
	@Column(name = "recommendation_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID recommendationId;

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "occurrence_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID occurrenceId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recommendation_id", insertable = false, updatable = false)
	private DailyTrainingRecommendationJpaEntity recommendation;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "training_plan_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID trainingPlanId;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "workout_day_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID workoutDayId;

	@Enumerated(EnumType.STRING)
	@Column(name = "occurrence_status", nullable = false, updatable = false, length = 32)
	private WorkoutOccurrenceStatus occurrenceStatus;

	@Column(name = "modifiable", nullable = false, updatable = false)
	private boolean modifiable;

	@Column(name = "planned_environment_name_snapshot", updatable = false, length = 120)
	private String plannedEnvironmentNameSnapshot;

	@Column(name = "actual_environment_name_snapshot", updatable = false, length = 120)
	private String actualEnvironmentNameSnapshot;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	protected DailyTrainingRecommendationOccurrenceJpaEntity() {
	}

	UUID getOccurrenceId() { return occurrenceId; }
	UUID getTrainingPlanId() { return trainingPlanId; }
	UUID getWorkoutDayId() { return workoutDayId; }
	WorkoutOccurrenceStatus getOccurrenceStatus() { return occurrenceStatus; }
	boolean isModifiable() { return modifiable; }
	String getPlannedEnvironmentNameSnapshot() { return plannedEnvironmentNameSnapshot; }
	String getActualEnvironmentNameSnapshot() { return actualEnvironmentNameSnapshot; }
	int getOrderIndex() { return orderIndex; }

	static DailyTrainingRecommendationOccurrenceJpaEntity of(
			DailyTrainingRecommendationJpaEntity recommendation,
			UUID occurrenceId,
			UUID trainingPlanId,
			UUID workoutDayId,
			WorkoutOccurrenceStatus occurrenceStatus,
			boolean modifiable,
			String plannedEnvironmentNameSnapshot,
			String actualEnvironmentNameSnapshot,
			int orderIndex) {
		DailyTrainingRecommendationOccurrenceJpaEntity entity = new DailyTrainingRecommendationOccurrenceJpaEntity();
		entity.recommendationId = recommendation.getId();
		entity.recommendation = recommendation;
		entity.occurrenceId = occurrenceId;
		entity.trainingPlanId = trainingPlanId;
		entity.workoutDayId = workoutDayId;
		entity.occurrenceStatus = occurrenceStatus;
		entity.modifiable = modifiable;
		entity.plannedEnvironmentNameSnapshot = plannedEnvironmentNameSnapshot;
		entity.actualEnvironmentNameSnapshot = actualEnvironmentNameSnapshot;
		entity.orderIndex = orderIndex;
		return entity;
	}

	public static final class Pk implements Serializable {
		private UUID recommendationId;
		private UUID occurrenceId;

		public Pk() {
		}

		public Pk(UUID recommendationId, UUID occurrenceId) {
			this.recommendationId = recommendationId;
			this.occurrenceId = occurrenceId;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return Objects.equals(recommendationId, pk.recommendationId)
					&& Objects.equals(occurrenceId, pk.occurrenceId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(recommendationId, occurrenceId);
		}
	}

}
