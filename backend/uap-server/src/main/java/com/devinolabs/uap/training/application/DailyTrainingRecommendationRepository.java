package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;

public interface DailyTrainingRecommendationRepository {

	DailyTrainingRecommendation saveNew(DailyTrainingRecommendation recommendation);

	Optional<DailyTrainingRecommendation> findByIdAndAthleteId(
			DailyTrainingRecommendationId id,
			AthleteId athleteId);

	Optional<DailyTrainingRecommendation> findByAssessmentIdAndAlgorithmVersion(
			DailyReadinessAssessmentId assessmentId,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			AthleteId athleteId);

	List<DailyTrainingRecommendationSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			TrainingRecommendationAction overallAction,
			int page,
			int size);

	long countHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			TrainingRecommendationAction overallAction);

}
