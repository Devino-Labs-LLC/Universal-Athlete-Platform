package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.devinolabs.uap.training.domain.TrainingAdjustmentType;

@Entity
@Table(name = "daily_training_recommendation_adjustments")
class DailyTrainingRecommendationAdjustmentJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "recommendation_id", nullable = false, updatable = false)
	private DailyTrainingRecommendationJpaEntity recommendation;

	@Enumerated(EnumType.STRING)
	@Column(name = "adjustment_type", nullable = false, updatable = false, length = 64)
	private TrainingAdjustmentType adjustmentType;

	@Column(name = "priority", nullable = false, updatable = false)
	private int priority;

	@Column(name = "explanation_key", nullable = false, updatable = false, length = 80)
	private String explanationKey;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	@OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 16)
	private List<DailyTrainingRecommendationAdjustmentReasonJpaEntity> reasons = new ArrayList<>();

	@OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 16)
	private List<DailyTrainingRecommendationAdjustmentDimensionJpaEntity> dimensions = new ArrayList<>();

	protected DailyTrainingRecommendationAdjustmentJpaEntity() {
	}

	UUID getId() { return id; }
	DailyTrainingRecommendationJpaEntity getRecommendation() { return recommendation; }
	TrainingAdjustmentType getAdjustmentType() { return adjustmentType; }
	int getPriority() { return priority; }
	String getExplanationKey() { return explanationKey; }
	int getOrderIndex() { return orderIndex; }
	List<DailyTrainingRecommendationAdjustmentReasonJpaEntity> getReasons() { return reasons; }
	List<DailyTrainingRecommendationAdjustmentDimensionJpaEntity> getDimensions() { return dimensions; }

	static DailyTrainingRecommendationAdjustmentJpaEntity of(
			DailyTrainingRecommendationJpaEntity recommendation,
			UUID id,
			TrainingAdjustmentType adjustmentType,
			int priority,
			String explanationKey,
			int orderIndex) {
		DailyTrainingRecommendationAdjustmentJpaEntity entity = new DailyTrainingRecommendationAdjustmentJpaEntity();
		entity.id = id;
		entity.recommendation = recommendation;
		entity.adjustmentType = adjustmentType;
		entity.priority = priority;
		entity.explanationKey = explanationKey;
		entity.orderIndex = orderIndex;
		return entity;
	}

}
