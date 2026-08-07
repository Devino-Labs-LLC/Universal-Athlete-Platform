package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;

@Service
public class GetDailyTrainingRecommendationUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public GetDailyTrainingRecommendationUseCase(
			AthleteContextPort athleteContextPort,
			DailyTrainingRecommendationRepository recommendationRepository,
			DailyReadinessAssessmentRepository assessmentRepository,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyTrainingRecommendationResult execute(AccountId accountId, UUID recommendationId) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyTrainingRecommendation recommendation = recommendationRepository
				.findByIdAndAthleteId(DailyTrainingRecommendationId.of(recommendationId), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationNotFoundException(
						"Daily training recommendation not found: " + recommendationId));
		DailyReadinessAssessment assessment = assessmentRepository
				.findByIdAndAthleteId(recommendation.dailyReadinessAssessmentId(), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
						"Source readiness assessment missing for recommendation " + recommendationId));
		DailyAthleteStateSnapshot snapshot = snapshotRepository
				.findByIdAndAthleteId(recommendation.dailyAthleteStateSnapshotId(), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
						"Source snapshot missing for recommendation " + recommendationId));
		return DailyTrainingRecommendationResult.from(recommendation, assessment, snapshot, false);
	}

}
