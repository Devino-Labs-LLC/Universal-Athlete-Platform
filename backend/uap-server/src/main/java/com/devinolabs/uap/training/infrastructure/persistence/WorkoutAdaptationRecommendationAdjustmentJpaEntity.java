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

import com.devinolabs.uap.training.domain.TrainingAdjustmentApplicability;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;

@Entity
@Table(name = "workout_adaptation_recommendation_adjustments")
class WorkoutAdaptationRecommendationAdjustmentJpaEntity {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "proposal_id", nullable = false, updatable = false)
	private WorkoutAdaptationProposalJpaEntity proposal;

	@Enumerated(EnumType.STRING)
	@Column(name = "training_adjustment_type", nullable = false, updatable = false, length = 64)
	private TrainingAdjustmentType trainingAdjustmentType;

	@Enumerated(EnumType.STRING)
	@Column(name = "applicability", nullable = false, updatable = false, length = 32)
	private TrainingAdjustmentApplicability applicability;

	@Column(name = "explanation_key", updatable = false, length = 80)
	private String explanationKey;

	@Column(name = "order_index", nullable = false, updatable = false)
	private int orderIndex;

	@OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<WorkoutAdaptationRecommendationAdjustmentReasonJpaEntity> reasons = new ArrayList<>();

	@OneToMany(mappedBy = "adjustment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	@OrderBy("orderIndex ASC")
	@BatchSize(size = 32)
	private List<WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity> dimensions = new ArrayList<>();

	protected WorkoutAdaptationRecommendationAdjustmentJpaEntity() {
	}

	UUID getId() { return id; }
	TrainingAdjustmentType getTrainingAdjustmentType() { return trainingAdjustmentType; }
	TrainingAdjustmentApplicability getApplicability() { return applicability; }
	String getExplanationKey() { return explanationKey; }
	int getOrderIndex() { return orderIndex; }
	List<WorkoutAdaptationRecommendationAdjustmentReasonJpaEntity> getReasons() { return reasons; }
	List<WorkoutAdaptationRecommendationAdjustmentDimensionJpaEntity> getDimensions() { return dimensions; }

	static WorkoutAdaptationRecommendationAdjustmentJpaEntity of(
			WorkoutAdaptationProposalJpaEntity proposal,
			UUID id,
			TrainingAdjustmentType trainingAdjustmentType,
			TrainingAdjustmentApplicability applicability,
			String explanationKey,
			int orderIndex) {
		WorkoutAdaptationRecommendationAdjustmentJpaEntity entity =
				new WorkoutAdaptationRecommendationAdjustmentJpaEntity();
		entity.id = id;
		entity.proposal = proposal;
		entity.trainingAdjustmentType = trainingAdjustmentType;
		entity.applicability = applicability;
		entity.explanationKey = explanationKey;
		entity.orderIndex = orderIndex;
		return entity;
	}

}
