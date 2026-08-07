package com.devinolabs.uap.training.application;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAdjustment;

@Service
public class CompareDailyTrainingRecommendationsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final DailyReadinessAssessmentRepository assessmentRepository;

	public CompareDailyTrainingRecommendationsUseCase(
			AthleteContextPort athleteContextPort,
			DailyTrainingRecommendationRepository recommendationRepository,
			DailyReadinessAssessmentRepository assessmentRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional(readOnly = true)
	public DailyTrainingRecommendationComparisonResult execute(
			AccountId accountId,
			UUID olderRecommendationId,
			UUID newerRecommendationId) {
		if (olderRecommendationId == null || newerRecommendationId == null) {
			throw new DailyTrainingRecommendationCompareInvalidException(
					"olderRecommendationId and newerRecommendationId are required");
		}
		if (olderRecommendationId.equals(newerRecommendationId)) {
			throw new DailyTrainingRecommendationCompareInvalidException(
					"olderRecommendationId and newerRecommendationId must be different");
		}
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyTrainingRecommendation older = recommendationRepository
				.findByIdAndAthleteId(DailyTrainingRecommendationId.of(olderRecommendationId), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationNotFoundException(
						"Older training recommendation not found: " + olderRecommendationId));
		DailyTrainingRecommendation newer = recommendationRepository
				.findByIdAndAthleteId(DailyTrainingRecommendationId.of(newerRecommendationId), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationNotFoundException(
						"Newer training recommendation not found: " + newerRecommendationId));

		DailyReadinessAssessment olderAssessment = assessmentRepository
				.findByIdAndAthleteId(older.dailyReadinessAssessmentId(), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
						"Older readiness assessment missing"));
		DailyReadinessAssessment newerAssessment = assessmentRepository
				.findByIdAndAthleteId(newer.dailyReadinessAssessmentId(), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
						"Newer readiness assessment missing"));

		Set<TrainingAdjustmentType> olderTypes = types(older);
		Set<TrainingAdjustmentType> newerTypes = types(newer);
		List<TrainingAdjustmentType> added = new ArrayList<>();
		List<TrainingAdjustmentType> removed = new ArrayList<>();
		for (TrainingAdjustmentType type : TrainingAdjustmentType.values()) {
			boolean inOlder = olderTypes.contains(type);
			boolean inNewer = newerTypes.contains(type);
			if (!inOlder && inNewer) {
				added.add(type);
			}
			else if (inOlder && !inNewer) {
				removed.add(type);
			}
		}

		List<ReadinessDimensionType> olderLimiting = olderAssessment.limitingDimensions();
		List<ReadinessDimensionType> newerLimiting = newerAssessment.limitingDimensions();

		return new DailyTrainingRecommendationComparisonResult(
				older.id().value(),
				newer.id().value(),
				older.stateDate(),
				newer.stateDate(),
				older.dailyReadinessAssessmentId().value(),
				newer.dailyReadinessAssessmentId().value(),
				older.dailyAthleteStateSnapshotId().value(),
				newer.dailyAthleteStateSnapshotId().value(),
				older.dailyAthleteStateSnapshotVersion(),
				newer.dailyAthleteStateSnapshotVersion(),
				older.overallAction() != newer.overallAction(),
				older.overallAction(),
				newer.overallAction(),
				List.copyOf(added),
				List.copyOf(removed),
				!olderLimiting.equals(newerLimiting),
				olderLimiting,
				newerLimiting);
	}

	private static Set<TrainingAdjustmentType> types(DailyTrainingRecommendation recommendation) {
		Set<TrainingAdjustmentType> types = EnumSet.noneOf(TrainingAdjustmentType.class);
		for (TrainingRecommendationAdjustment adjustment : recommendation.adjustments()) {
			types.add(adjustment.type());
		}
		return types;
	}

}
