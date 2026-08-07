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

import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;
import com.devinolabs.uap.training.infrastructure.persistence.DailyTrainingRecommendationAdjustmentReasonJpaEntity.Pk;

@Entity
@Table(name = "daily_training_recommendation_adjustment_reasons")
@IdClass(Pk.class)
class DailyTrainingRecommendationAdjustmentReasonJpaEntity {

	@Id
	@Column(name = "adjustment_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID adjustmentId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", nullable = false, length = 64)
	private TrainingRecommendationReasonCode reasonCode;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "adjustment_id", insertable = false, updatable = false)
	private DailyTrainingRecommendationAdjustmentJpaEntity adjustment;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	protected DailyTrainingRecommendationAdjustmentReasonJpaEntity() {
	}

	TrainingRecommendationReasonCode getReasonCode() { return reasonCode; }
	int getOrderIndex() { return orderIndex; }

	static DailyTrainingRecommendationAdjustmentReasonJpaEntity of(
			DailyTrainingRecommendationAdjustmentJpaEntity adjustment,
			TrainingRecommendationReasonCode reasonCode,
			int orderIndex) {
		DailyTrainingRecommendationAdjustmentReasonJpaEntity entity =
				new DailyTrainingRecommendationAdjustmentReasonJpaEntity();
		entity.adjustmentId = adjustment.getId();
		entity.adjustment = adjustment;
		entity.reasonCode = reasonCode;
		entity.orderIndex = orderIndex;
		return entity;
	}

	public static final class Pk implements Serializable {
		private UUID adjustmentId;
		private TrainingRecommendationReasonCode reasonCode;

		public Pk() {
		}

		public Pk(UUID adjustmentId, TrainingRecommendationReasonCode reasonCode) {
			this.adjustmentId = adjustmentId;
			this.reasonCode = reasonCode;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Pk pk)) {
				return false;
			}
			return Objects.equals(adjustmentId, pk.adjustmentId) && reasonCode == pk.reasonCode;
		}

		@Override
		public int hashCode() {
			return Objects.hash(adjustmentId, reasonCode);
		}
	}

}
