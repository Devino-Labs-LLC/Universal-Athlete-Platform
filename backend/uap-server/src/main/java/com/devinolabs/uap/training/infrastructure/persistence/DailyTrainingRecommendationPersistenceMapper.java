package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.devinolabs.uap.training.application.DailyTrainingRecommendationSummary;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAdjustment;
import com.devinolabs.uap.training.domain.TrainingRecommendationOccurrenceContext;
import com.devinolabs.uap.training.domain.TrainingRecommendationReasonCode;

final class DailyTrainingRecommendationPersistenceMapper {

	private DailyTrainingRecommendationPersistenceMapper() {
	}

	static DailyTrainingRecommendationJpaEntity toNewEntity(DailyTrainingRecommendation recommendation) {
		DailyTrainingRecommendationJpaEntity entity = DailyTrainingRecommendationJpaEntity.createNew(
				recommendation.id().value(),
				recommendation.athleteId().value(),
				recommendation.stateDate(),
				recommendation.dailyReadinessAssessmentId().value(),
				recommendation.dailyAthleteStateSnapshotId().value(),
				recommendation.dailyAthleteStateSnapshotVersion(),
				recommendation.recommendationAlgorithmVersion(),
				recommendation.overallAction(),
				recommendation.recommendationStatus(),
				recommendation.primaryReasonCode(),
				recommendation.scheduledTrainingPresent(),
				recommendation.scheduledOccurrenceCount(),
				recommendation.modifiableScheduledOccurrenceCount(),
				recommendation.adjustmentCount(),
				recommendation.limitingDimensionCount(),
				recommendation.generatedAt(),
				recommendation.createdAt());

		for (TrainingRecommendationAdjustment adjustment : recommendation.adjustments()) {
			DailyTrainingRecommendationAdjustmentJpaEntity adjustmentEntity =
					DailyTrainingRecommendationAdjustmentJpaEntity.of(
							entity,
							adjustment.id(),
							adjustment.type(),
							adjustment.priority(),
							adjustment.explanationKey(),
							adjustment.orderIndex());
			int reasonOrder = 0;
			for (TrainingRecommendationReasonCode reason : adjustment.reasonCodes()) {
				adjustmentEntity.getReasons().add(
						DailyTrainingRecommendationAdjustmentReasonJpaEntity.of(
								adjustmentEntity, reason, reasonOrder++));
			}
			int dimensionOrder = 0;
			for (ReadinessDimensionType dimension : adjustment.sourceDimensions()) {
				adjustmentEntity.getDimensions().add(
						DailyTrainingRecommendationAdjustmentDimensionJpaEntity.of(
								adjustmentEntity, dimension, dimensionOrder++));
			}
			entity.getAdjustments().add(adjustmentEntity);
		}

		for (TrainingRecommendationOccurrenceContext occurrence : recommendation.occurrenceContexts()) {
			entity.getOccurrences().add(DailyTrainingRecommendationOccurrenceJpaEntity.of(
					entity,
					occurrence.occurrenceId(),
					occurrence.trainingPlanId(),
					occurrence.workoutDayId(),
					occurrence.occurrenceStatus(),
					occurrence.modifiable(),
					occurrence.plannedEnvironmentNameSnapshot(),
					occurrence.actualEnvironmentNameSnapshot(),
					occurrence.orderIndex()));
		}
		return entity;
	}

	static DailyTrainingRecommendation toDomain(DailyTrainingRecommendationJpaEntity entity) {
		List<TrainingRecommendationAdjustment> adjustments = new ArrayList<>();
		entity.getAdjustments().stream()
				.sorted(Comparator.comparingInt(DailyTrainingRecommendationAdjustmentJpaEntity::getOrderIndex))
				.forEach(row -> {
					row.getReasons().size();
					row.getDimensions().size();
					List<TrainingRecommendationReasonCode> reasons = row.getReasons().stream()
							.sorted(Comparator.comparingInt(
									DailyTrainingRecommendationAdjustmentReasonJpaEntity::getOrderIndex))
							.map(DailyTrainingRecommendationAdjustmentReasonJpaEntity::getReasonCode)
							.toList();
					List<ReadinessDimensionType> dimensions = row.getDimensions().stream()
							.sorted(Comparator.comparingInt(
									DailyTrainingRecommendationAdjustmentDimensionJpaEntity::getOrderIndex))
							.map(DailyTrainingRecommendationAdjustmentDimensionJpaEntity::getDimensionType)
							.toList();
					adjustments.add(new TrainingRecommendationAdjustment(
							row.getId(),
							row.getAdjustmentType(),
							row.getPriority(),
							reasons,
							dimensions,
							row.getExplanationKey(),
							row.getOrderIndex()));
				});

		List<TrainingRecommendationOccurrenceContext> occurrences = entity.getOccurrences().stream()
				.sorted(Comparator.comparingInt(DailyTrainingRecommendationOccurrenceJpaEntity::getOrderIndex))
				.map(row -> new TrainingRecommendationOccurrenceContext(
						row.getOccurrenceId(),
						row.getTrainingPlanId(),
						row.getWorkoutDayId(),
						row.getOccurrenceStatus(),
						row.isModifiable(),
						row.getPlannedEnvironmentNameSnapshot(),
						row.getActualEnvironmentNameSnapshot(),
						row.getOrderIndex()))
				.toList();

		return DailyTrainingRecommendation.rehydrate(
				DailyTrainingRecommendationId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getStateDate(),
				DailyReadinessAssessmentId.of(entity.getDailyReadinessAssessmentId()),
				DailyAthleteStateSnapshotId.of(entity.getDailyAthleteStateSnapshotId()),
				entity.getDailyAthleteStateSnapshotVersion(),
				entity.getRecommendationAlgorithmVersion(),
				entity.getOverallAction(),
				entity.getRecommendationStatus(),
				entity.getPrimaryReasonCode(),
				entity.isScheduledTrainingPresent(),
				entity.getScheduledOccurrenceCount(),
				entity.getModifiableScheduledOccurrenceCount(),
				entity.getAdjustmentCount(),
				entity.getLimitingDimensionCount(),
				entity.getGeneratedAt(),
				entity.getCreatedAt(),
				adjustments,
				occurrences);
	}

	static DailyTrainingRecommendationSummary toSummary(
			DailyTrainingRecommendationJpaEntity entity,
			boolean currentSnapshot) {
		return new DailyTrainingRecommendationSummary(
				entity.getId(),
				entity.getStateDate(),
				entity.getDailyReadinessAssessmentId(),
				entity.getDailyAthleteStateSnapshotId(),
				entity.getDailyAthleteStateSnapshotVersion(),
				currentSnapshot,
				entity.getRecommendationAlgorithmVersion(),
				entity.getOverallAction(),
				entity.getRecommendationStatus(),
				entity.getPrimaryReasonCode(),
				entity.getAdjustmentCount(),
				entity.getGeneratedAt());
	}

}
