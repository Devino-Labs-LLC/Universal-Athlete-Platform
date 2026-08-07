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

import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.infrastructure.persistence.WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity.Pk;

@Entity
@Table(name = "workout_adaptation_recommendation_adjustment_dimensions")
@IdClass(Pk.class)
class WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity {

	@Id
	@Column(name = "adjustment_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID adjustmentId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "dimension_type", nullable = false, length = 40)
	private ReadinessDimensionType dimensionType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "adjustment_id", insertable = false, updatable = false)
	private WorkoutAdaptationRecommendationAdjustmentJpaEntity adjustment;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	protected WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity() {
	}

	ReadinessDimensionType getDimensionType() { return dimensionType; }
	int getOrderIndex() { return orderIndex; }

	static WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity of(
			WorkoutAdaptationRecommendationAdjustmentJpaEntity adjustment,
			ReadinessDimensionType dimensionType,
			int orderIndex) {
		WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity entity =
				new WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity();
		entity.adjustmentId = adjustment.getId();
		entity.adjustment = adjustment;
		entity.dimensionType = dimensionType;
		entity.orderIndex = orderIndex;
		return entity;
	}

	public static final class Pk implements Serializable {
		private UUID adjustmentId;
		private ReadinessDimensionType dimensionType;

		public Pk() {
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return Objects.equals(adjustmentId, pk.adjustmentId) && dimensionType == pk.dimensionType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(adjustmentId, dimensionType);
		}
	}

}
