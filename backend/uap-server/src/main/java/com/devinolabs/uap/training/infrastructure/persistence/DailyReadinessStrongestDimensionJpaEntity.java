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
import com.devinolabs.uap.training.infrastructure.persistence.DailyReadinessStrongestDimensionJpaEntity.Pk;

@Entity
@Table(name = "daily_readiness_strongest_dimensions")
@IdClass(Pk.class)
class DailyReadinessStrongestDimensionJpaEntity {

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

	@Column(name = "rank_order", nullable = false)
	private int rankOrder;

	protected DailyReadinessStrongestDimensionJpaEntity() {
	}

	static DailyReadinessStrongestDimensionJpaEntity of(
			DailyReadinessAssessmentJpaEntity assessment,
			ReadinessDimensionType dimensionType,
			int rankOrder) {
		DailyReadinessStrongestDimensionJpaEntity entity = new DailyReadinessStrongestDimensionJpaEntity();
		entity.assessment = assessment;
		entity.assessmentId = assessment.getId();
		entity.dimensionType = dimensionType;
		entity.rankOrder = rankOrder;
		return entity;
	}

	ReadinessDimensionType getDimensionType() { return dimensionType; }
	int getRankOrder() { return rankOrder; }

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
