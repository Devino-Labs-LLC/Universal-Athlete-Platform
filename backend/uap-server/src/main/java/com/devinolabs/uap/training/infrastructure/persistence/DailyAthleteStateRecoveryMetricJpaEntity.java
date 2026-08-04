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

import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricDirection;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.infrastructure.persistence.DailyAthleteStateRecoveryMetricJpaEntity.Pk;

@Entity
@Table(name = "daily_athlete_state_recovery_metrics")
@IdClass(Pk.class)
class DailyAthleteStateRecoveryMetricJpaEntity {

	@Id
	@Column(name = "snapshot_id", nullable = false, columnDefinition = "BINARY(16)")
	private UUID snapshotId;

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "metric_type", nullable = false, length = 40)
	private RecoveryMetricType metricType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "snapshot_id", insertable = false, updatable = false)
	private DailyAthleteStateSnapshotJpaEntity snapshot;

	@Column(name = "target_value", precision = 18, scale = 4)
	private BigDecimal targetValue;

	@Enumerated(EnumType.STRING)
	@Column(name = "metric_direction", nullable = false, length = 40)
	private RecoveryMetricDirection metricDirection;

	@Column(name = "observation_count", nullable = false)
	private int observationCount;

	@Enumerated(EnumType.STRING)
	@Column(name = "data_sufficiency", nullable = false, length = 16)
	private RecoveryBaselineDataSufficiency dataSufficiency;

	@Column(name = "baseline_mean", precision = 18, scale = 2)
	private BigDecimal baselineMean;

	@Column(name = "baseline_median", precision = 18, scale = 2)
	private BigDecimal baselineMedian;

	@Column(name = "baseline_minimum", precision = 18, scale = 4)
	private BigDecimal baselineMinimum;

	@Column(name = "baseline_maximum", precision = 18, scale = 4)
	private BigDecimal baselineMaximum;

	@Column(name = "baseline_standard_deviation", precision = 18, scale = 4)
	private BigDecimal baselineStandardDeviation;

	@Column(name = "absolute_difference", precision = 18, scale = 4)
	private BigDecimal absoluteDifference;

	@Column(name = "percentage_difference", precision = 18, scale = 4)
	private BigDecimal percentageDifference;

	@Column(name = "standardized_deviation", precision = 18, scale = 4)
	private BigDecimal standardizedDeviation;

	@Enumerated(EnumType.STRING)
	@Column(name = "comparison_band", nullable = false, length = 40)
	private RecoveryComparisonBand comparisonBand;

	@Enumerated(EnumType.STRING)
	@Column(name = "reason_code", length = 64)
	private RecoveryAnalyticsReasonCode reasonCode;

	protected DailyAthleteStateRecoveryMetricJpaEntity() {
	}

	static DailyAthleteStateRecoveryMetricJpaEntity of(
			DailyAthleteStateSnapshotJpaEntity snapshot,
			RecoveryMetricType metricType,
			BigDecimal targetValue,
			RecoveryMetricDirection metricDirection,
			int observationCount,
			RecoveryBaselineDataSufficiency dataSufficiency,
			BigDecimal baselineMean,
			BigDecimal baselineMedian,
			BigDecimal baselineMinimum,
			BigDecimal baselineMaximum,
			BigDecimal baselineStandardDeviation,
			BigDecimal absoluteDifference,
			BigDecimal percentageDifference,
			BigDecimal standardizedDeviation,
			RecoveryComparisonBand comparisonBand,
			RecoveryAnalyticsReasonCode reasonCode) {
		DailyAthleteStateRecoveryMetricJpaEntity entity = new DailyAthleteStateRecoveryMetricJpaEntity();
		entity.snapshot = snapshot;
		entity.snapshotId = snapshot.getId();
		entity.metricType = metricType;
		entity.targetValue = targetValue;
		entity.metricDirection = metricDirection;
		entity.observationCount = observationCount;
		entity.dataSufficiency = dataSufficiency;
		entity.baselineMean = baselineMean;
		entity.baselineMedian = baselineMedian;
		entity.baselineMinimum = baselineMinimum;
		entity.baselineMaximum = baselineMaximum;
		entity.baselineStandardDeviation = baselineStandardDeviation;
		entity.absoluteDifference = absoluteDifference;
		entity.percentageDifference = percentageDifference;
		entity.standardizedDeviation = standardizedDeviation;
		entity.comparisonBand = comparisonBand;
		entity.reasonCode = reasonCode;
		return entity;
	}

	RecoveryMetricType getMetricType() { return metricType; }
	BigDecimal getTargetValue() { return targetValue; }
	RecoveryMetricDirection getMetricDirection() { return metricDirection; }
	int getObservationCount() { return observationCount; }
	RecoveryBaselineDataSufficiency getDataSufficiency() { return dataSufficiency; }
	BigDecimal getBaselineMean() { return baselineMean; }
	BigDecimal getBaselineMedian() { return baselineMedian; }
	BigDecimal getBaselineMinimum() { return baselineMinimum; }
	BigDecimal getBaselineMaximum() { return baselineMaximum; }
	BigDecimal getBaselineStandardDeviation() { return baselineStandardDeviation; }
	BigDecimal getAbsoluteDifference() { return absoluteDifference; }
	BigDecimal getPercentageDifference() { return percentageDifference; }
	BigDecimal getStandardizedDeviation() { return standardizedDeviation; }
	RecoveryComparisonBand getComparisonBand() { return comparisonBand; }
	RecoveryAnalyticsReasonCode getReasonCode() { return reasonCode; }

	public static final class Pk implements Serializable {
		private UUID snapshotId;
		private RecoveryMetricType metricType;

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
			return Objects.equals(snapshotId, that.snapshotId) && metricType == that.metricType;
		}

		@Override
		public int hashCode() {
			return Objects.hash(snapshotId, metricType);
		}
	}

}
