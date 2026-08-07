package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.TrainingRecommendationCalculator;

/**
 * Generates an immutable training recommendation from one DailyReadinessAssessment
 * and its source DailyAthleteStateSnapshot. Does not query live recovery/load sources.
 */
@Service
public class GenerateDailyTrainingRecommendationUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyTrainingRecommendationRepository recommendationRepository;
	private final Clock clock;

	public GenerateDailyTrainingRecommendationUseCase(
			AthleteContextPort athleteContextPort,
			DailyReadinessAssessmentRepository assessmentRepository,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyTrainingRecommendationRepository recommendationRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public DailyTrainingRecommendationResult execute(AccountId accountId, UUID dailyReadinessAssessmentId) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyReadinessAssessment assessment = assessmentRepository
				.findByIdAndAthleteId(DailyReadinessAssessmentId.of(dailyReadinessAssessmentId), athleteId)
				.orElseThrow(() -> new DailyReadinessAssessmentNotFoundException(
						"Daily readiness assessment not found: " + dailyReadinessAssessmentId));

		Optional<DailyTrainingRecommendation> existing = recommendationRepository
				.findByAssessmentIdAndAlgorithmVersion(
						assessment.id(),
						TrainingRecommendationCalculator.ALGORITHM_VERSION,
						athleteId);
		DailyAthleteStateSnapshot snapshot = requireSnapshot(assessment, athleteId);
		if (existing.isPresent()) {
			return DailyTrainingRecommendationResult.from(existing.get(), assessment, snapshot, false);
		}

		try {
			TrainingRecommendationCalculator.CalculationResult calculation =
					TrainingRecommendationCalculator.calculate(assessment, snapshot, clock);
			DailyTrainingRecommendation created = DailyTrainingRecommendation.create(assessment, calculation);
			DailyTrainingRecommendation saved = recommendationRepository.saveNew(created);
			return DailyTrainingRecommendationResult.from(saved, assessment, snapshot, true);
		}
		catch (DataIntegrityViolationException ex) {
			return recommendationRepository.findByAssessmentIdAndAlgorithmVersion(
							assessment.id(),
							TrainingRecommendationCalculator.ALGORITHM_VERSION,
							athleteId)
					.map(recommendation -> DailyTrainingRecommendationResult.from(
							recommendation, assessment, snapshot, false))
					.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
							"Concurrent training recommendation generation conflict", ex));
		}
		catch (RuntimeException ex) {
			throw new DailyTrainingRecommendationCalculationFailedException(
					"Failed to generate daily training recommendation", ex);
		}
	}

	private DailyAthleteStateSnapshot requireSnapshot(
			DailyReadinessAssessment assessment,
			AthleteId athleteId) {
		return snapshotRepository
				.findByIdAndAthleteId(assessment.dailyAthleteStateSnapshotId(), athleteId)
				.orElseThrow(() -> new DailyTrainingRecommendationCalculationFailedException(
						"Source daily athlete state snapshot not found for readiness assessment "
								+ assessment.id().value()));
	}

}
