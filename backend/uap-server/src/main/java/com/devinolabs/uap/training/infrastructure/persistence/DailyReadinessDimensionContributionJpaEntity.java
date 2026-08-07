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

import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessReasonCode;
import com.devinolabs.uap.training.infrastructure.persistence.DailyReadinessDimensionContributionJpaEntity.Pk;

@Entity
@Table(name = "daily_readiness_dimension_contributions")
@IdClass(Pk.class)
class DailyReadinessDimensionContributionJpaEntity {

	@Id
	@Column(name = "assessment_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID assessmentId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "dimension_type", nullable = false, length = 40)
	private ReadinessDimensionType dimensionType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "assessment_id", insertable = false, updatable = false)
	private DailyReadinessAssessmentJpaEntity assessment;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_metric_type", length = 40)
	private RecoveryMetricType sourceMetricType;

	@Column(name = "available", nullable = false)
	private boolean available;

	@Enumerated(EnumType.STRING)
	@Column(name = "baseline_sufficiency", length = 16)
	private RecoveryBaselineDataSufficiency baselineSufficiency;

	@Column(name = "target_value", precision = 18, scale = 4)
	private BigDecimal targetValue;

	@Column(name = "baseline_mean", precision = 18, scale = 2)
	private BigDecimal baselineMean;

	@Column(name = "standardized_deviation", precision = 18, scale = 4)
	private BigDecimal standardizedDeviation;

	@Enumerated(EnumType.STRING)
	@Column(name = "comparison_band", length = 40)
	private RecoveryComparisonBand comparisonBand;

	@Column(name = "normalized_score", precision = 5, scale = 2)
	private BigDecimal normalizedScore;

	@Column(name = "configured_weight", nullable = false, precision = 6, scale = 5)
	private BigDecimal configuredWeight;

	@Column(name = "effective_weight", precision = 6, scale = 5)
	private BigDecimal effectiveWeight;

	@Column(name = "weighted_contribution", precision = 8, scale = 4)
	private BigDecimal weightedContribution;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", nullable = false, length = 64)
	private ReadinessReasonCode reasonCode;

	@Column(name = "rank_as_limiting")
	private Integer rankAsLimiting;

	@Column(name = "rank_as_strongest")
	private Integer rankAsStrongest;

	protected DailyReadinessDimensionContributionJpaEntity() {
	}

	static DailyReadinessDimensionContributionJpaEntity of(
			DailyReadinessAssessmentJpaEntity assessment,
			ReadinessDimensionType dimensionType,
			RecoveryMetricType sourceMetricType,
			boolean available,
			RecoveryBaselineDataSufficiency baselineSufficiency,
			BigDecimal targetValue,
			BigDecimal baselineMean,
			BigDecimal standardizedDeviation,
			RecoveryComparisonBand comparisonBand,
			BigDecimal normalizedScore,
			BigDecimal configuredWeight,
			BigDecimal effectiveWeight,
			BigDecimal weightedContribution,
			ReadinessReasonCode reasonCode,
			Integer rankAsLimiting,
			Integer rankAsStrongest) {
		DailyReadinessDimensionContributionJpaEntity entity = new DailyReadinessDimensionContributionJpaEntity();
		entity.assessment = assessment;
		entity.assessmentId = assessment.getId();
		entity.dimensionType = dimensionType;
		entity.sourceMetricType = sourceMetricType;
		entity.available = available;
		entity.baselineSufficiency = baselineSufficiency;
		entity.targetValue = targetValue;
		entity.baselineMean = baselineMean;
		entity.standardizedDeviation = standardizedDeviation;
		entity.comparisonBand = comparisonBand;
		entity.normalizedScore = normalizedScore;
		entity.configuredWeight = configuredWeight;
		entity.effectiveWeight = effectiveWeight;
		entity.weightedContribution = weightedContribution;
		entity.reasonCode = reasonCode;
		entity.rankAsLimiting = rankAsLimiting;
		entity.rankAsStrongest = rankAsStrongest;
		return entity;
	}

	ReadinessDimensionType getDimensionType() { return dimensionType; }
	RecoveryMetricType getSourceMetricType() { return sourceMetricType; }
	boolean isAvailable() { return available; }
	RecoveryBaselineDataSufficiency getBaselineSufficiency() { return baselineSufficiency; }
	BigDecimal getTargetValue() { return targetValue; }
	BigDecimal getBaselineMean() { return baselineMean; }
	BigDecimal getStandardizedDeviation() { return standardizedDeviation; }
	RecoveryComparisonBand getComparisonBand() { return comparisonBand; }
	BigDecimal getNormalizedScore() { return normalizedScore; }
	BigDecimal getConfiguredWeight() { return configuredWeight; }
	BigDecimal getEffectiveWeight() { return effectiveWeight; }
	BigDecimal getWeightedContribution() { return weightedContribution; }
	ReadinessReasonCode getReasonCode() { return reasonCode; }
	Integer getRankAsLimiting() { return rankAsLimiting; }
	Integer getRankAsStrongest() { return rankAsStrongest; }

	public static final class Pk implements Serializable {
		private UUID assessmentId;
		private ReadinessDimensionType dimensionType;

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
			return Objects.equals(assessmentId, that.assessmentId) && dimensionType == that.dimensionType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(assessmentId, dimensionType);
		}
	}

}
