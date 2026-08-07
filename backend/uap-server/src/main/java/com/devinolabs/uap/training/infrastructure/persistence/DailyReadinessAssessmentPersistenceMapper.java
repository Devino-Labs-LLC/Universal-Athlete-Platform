package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.devinolabs.uap.training.application.DailyReadinessAssessmentSummary;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.ReadinessDimensionContribution;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.ReadinessScore;

final class DailyReadinessAssessmentPersistenceMapper {

	private DailyReadinessAssessmentPersistenceMapper() {
	}

	static DailyReadinessAssessmentJpaEntity toNewEntity(DailyReadinessAssessment assessment) {
		DailyReadinessAssessmentJpaEntity entity = DailyReadinessAssessmentJpaEntity.createNew(
				assessment.id().value(),
				assessment.athleteId().value(),
				assessment.stateDate(),
				assessment.dailyAthleteStateSnapshotId().value(),
				assessment.dailyAthleteStateSnapshotVersion(),
				assessment.algorithmVersion(),
				assessment.scoreValue(),
				assessment.readinessBand(),
				assessment.dataSufficiency(),
				assessment.summaryReasonCode(),
				assessment.limitingDimensionCount(),
				assessment.contributingDimensionCount(),
				assessment.assessedAt(),
				assessment.createdAt());
		for (ReadinessDimensionContribution contribution : assessment.contributions()) {
			entity.getContributions().add(DailyReadinessDimensionContributionJpaEntity.of(
					entity,
					contribution.dimensionType(),
					contribution.sourceMetricType(),
					contribution.available(),
					contribution.baselineSufficiency(),
					contribution.targetValue(),
					contribution.baselineMean(),
					contribution.standardizedDeviation(),
					contribution.comparisonBand(),
					contribution.normalizedScore(),
					contribution.configuredWeight(),
					contribution.effectiveWeight(),
					contribution.weightedContribution(),
					contribution.reasonCode(),
					contribution.rankAsLimiting(),
					contribution.rankAsStrongest()));
		}
		int limitingRank = 1;
		for (ReadinessDimensionType dimension : assessment.limitingDimensions()) {
			entity.getLimitingDimensions().add(
					DailyReadinessLimitingDimensionJpaEntity.of(entity, dimension, limitingRank++));
		}
		int strongestRank = 1;
		for (ReadinessDimensionType dimension : assessment.strongestDimensions()) {
			entity.getStrongestDimensions().add(
					DailyReadinessStrongestDimensionJpaEntity.of(entity, dimension, strongestRank++));
		}
		return entity;
	}

	static DailyReadinessAssessment toDomain(DailyReadinessAssessmentJpaEntity entity) {
		List<ReadinessDimensionContribution> contributions = new ArrayList<>();
		entity.getContributions().stream()
				.sorted(Comparator.comparing(c -> c.getDimensionType().name()))
				.forEach(row -> contributions.add(new ReadinessDimensionContribution(
						row.getDimensionType(),
						row.getSourceMetricType(),
						row.isAvailable(),
						row.getBaselineSufficiency(),
						row.getTargetValue(),
						row.getBaselineMean(),
						row.getStandardizedDeviation(),
						row.getComparisonBand(),
						row.getNormalizedScore(),
						row.getConfiguredWeight(),
						row.getEffectiveWeight(),
						row.getWeightedContribution(),
						row.getReasonCode(),
						row.getRankAsLimiting(),
						row.getRankAsStrongest())));
		List<ReadinessDimensionType> limiting = entity.getLimitingDimensions().stream()
				.sorted(Comparator.comparingInt(DailyReadinessLimitingDimensionJpaEntity::getRankOrder))
				.map(DailyReadinessLimitingDimensionJpaEntity::getDimensionType)
				.toList();
		List<ReadinessDimensionType> strongest = entity.getStrongestDimensions().stream()
				.sorted(Comparator.comparingInt(DailyReadinessStrongestDimensionJpaEntity::getRankOrder))
				.map(DailyReadinessStrongestDimensionJpaEntity::getDimensionType)
				.toList();
		ReadinessScore score = entity.getReadinessScore() == null
				? null
				: ReadinessScore.of(entity.getReadinessScore());
		return DailyReadinessAssessment.rehydrate(
				DailyReadinessAssessmentId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getStateDate(),
				DailyAthleteStateSnapshotId.of(entity.getDailyAthleteStateSnapshotId()),
				entity.getDailyAthleteStateSnapshotVersion(),
				entity.getAlgorithmVersion(),
				score,
				entity.getReadinessBand(),
				entity.getDataSufficiency(),
				entity.getSummaryReasonCode(),
				entity.getLimitingDimensionCount(),
				entity.getContributingDimensionCount(),
				entity.getAssessedAt(),
				entity.getCreatedAt(),
				contributions,
				limiting,
				strongest);
	}

	static DailyReadinessAssessmentSummary toSummary(
			DailyReadinessAssessmentJpaEntity entity,
			boolean currentSnapshot) {
		return new DailyReadinessAssessmentSummary(
				entity.getId(),
				entity.getStateDate(),
				entity.getDailyAthleteStateSnapshotId(),
				entity.getDailyAthleteStateSnapshotVersion(),
				currentSnapshot,
				entity.getAlgorithmVersion(),
				entity.getReadinessScore(),
				entity.getReadinessBand(),
				entity.getDataSufficiency(),
				entity.getSummaryReasonCode(),
				entity.getAssessedAt());
	}

}
