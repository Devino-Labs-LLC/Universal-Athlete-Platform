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
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.ReadinessCalculator;
import com.devinolabs.uap.training.domain.ReadinessNumericOverflowException;

/**
 * Generates an immutable readiness assessment from one DailyAthleteStateSnapshot.
 * Does not query live recovery/load/schedule sources.
 */
@Service
public class GenerateDailyReadinessAssessmentUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final Clock clock;

	public GenerateDailyReadinessAssessmentUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyReadinessAssessmentRepository assessmentRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public DailyReadinessAssessmentResult execute(AccountId accountId, UUID dailyAthleteStateSnapshotId) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyAthleteStateSnapshot snapshot = snapshotRepository
				.findByIdAndAthleteId(DailyAthleteStateSnapshotId.of(dailyAthleteStateSnapshotId), athleteId)
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Daily athlete state snapshot not found: " + dailyAthleteStateSnapshotId));

		Optional<DailyReadinessAssessment> existing = assessmentRepository.findBySnapshotIdAndAlgorithmVersion(
				snapshot.id(), ReadinessCalculator.ALGORITHM_VERSION, athleteId);
		if (existing.isPresent()) {
			return DailyReadinessAssessmentResult.from(existing.get(), snapshot, false);
		}

		try {
			ReadinessCalculator.CalculationResult calculation = ReadinessCalculator.calculate(snapshot, clock);
			DailyReadinessAssessment created = DailyReadinessAssessment.create(snapshot, calculation);
			DailyReadinessAssessment saved = assessmentRepository.saveNew(created);
			return DailyReadinessAssessmentResult.from(saved, snapshot, true);
		}
		catch (DataIntegrityViolationException ex) {
			return assessmentRepository.findBySnapshotIdAndAlgorithmVersion(
							snapshot.id(), ReadinessCalculator.ALGORITHM_VERSION, athleteId)
					.map(assessment -> DailyReadinessAssessmentResult.from(assessment, snapshot, false))
					.orElseThrow(() -> new DailyReadinessCalculationFailedException(
							"Concurrent readiness assessment generation conflict", ex));
		}
		catch (ReadinessNumericOverflowException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new DailyReadinessCalculationFailedException("Failed to generate daily readiness assessment", ex);
		}
	}

}
