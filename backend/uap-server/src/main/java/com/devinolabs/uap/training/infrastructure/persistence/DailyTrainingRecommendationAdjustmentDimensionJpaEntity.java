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
import com.devinolabs.uap.training.infrastructure.persistence.DailyTrainingRecommendationAdjustmentDimensionJpaEntity.Pk;

@Entity
@Table(name = "daily_training_recommendation_adjustment_dimensions")
@IdClass(Pk.class)
class DailyTrainingRecommendationAdjustmentDimensionJpaEntity {

	@Id
	@Column(name = "adjustment_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID adjustmentId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "dimension_type", nullable = false, length = 40)
	private ReadinessDimensionType dimensionType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "adjustment_id", insertable = false, updatable = false)
	private DailyTrainingRecommendationAdjustmentJpaEntity adjustment;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	protected DailyTrainingRecommendationAdjustmentDimensionJpaEntity() {
	}

	ReadinessDimensionType getDimensionType() { return dimensionType; }
	int getOrderIndex() { return orderIndex; }

	static DailyTrainingRecommendationAdjustmentDimensionJpaEntity of(
			DailyTrainingRecommendationAdjustmentJpaEntity adjustment,
			ReadinessDimensionType dimensionType,
			int orderIndex) {
		DailyTrainingRecommendationAdjustmentDimensionJpaEntity entity =
				new DailyTrainingRecommendationAdjustmentDimensionJpaEntity();
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

		public Pk(UUID adjustmentId, ReadinessDimensionType dimensionType) {
			this.adjustmentId = adjustmentId;
			this.dimensionType = dimensionType;
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
