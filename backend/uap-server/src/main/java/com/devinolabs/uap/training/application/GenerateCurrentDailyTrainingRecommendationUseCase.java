package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.ReadinessCalculator;

@Service
public class GenerateCurrentDailyTrainingRecommendationUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final GenerateDailyTrainingRecommendationUseCase generateDailyTrainingRecommendationUseCase;

	public GenerateCurrentDailyTrainingRecommendationUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyReadinessAssessmentRepository assessmentRepository,
			GenerateDailyTrainingRecommendationUseCase generateDailyTrainingRecommendationUseCase) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.generateDailyTrainingRecommendationUseCase =
				Objects.requireNonNull(generateDailyTrainingRecommendationUseCase);
	}

	@Transactional
	public DailyTrainingRecommendationResult execute(AccountId accountId, LocalDate stateDate) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyAthleteStateSnapshot snapshot = snapshotRepository
				.findCurrentByAthleteIdAndStateDate(athleteId, stateDate)
				.orElseThrow(() -> new DailyTrainingRecommendationReadinessRequiredException(
						"A current daily athlete state snapshot and READINESS_V1 assessment are required for date "
								+ stateDate));
		DailyReadinessAssessment assessment = assessmentRepository
				.findBySnapshotIdAndAlgorithmVersion(
						snapshot.id(),
						ReadinessCalculator.ALGORITHM_VERSION,
						athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationReadinessRequiredException(
						"A READINESS_V1 assessment is required for the current snapshot on " + stateDate));
		return generateDailyTrainingRecommendationUseCase.execute(accountId, assessment.id().value());
	}

}
