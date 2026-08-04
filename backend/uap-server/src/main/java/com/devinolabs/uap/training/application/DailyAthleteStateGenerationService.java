package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateDateOutOfRangeException;
import com.devinolabs.uap.training.domain.DailyAthleteStateFingerprintCalculator;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotFactory;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotFactory.AssembledDailyAthleteStateSource;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateBaselineWindowException;
import com.devinolabs.uap.training.domain.InvalidDailyAthleteStateDateException;

/**
 * Explicit snapshot generation only. Phase 7Q does not auto-refresh after source mutations.
 */
@Service
class DailyAthleteStateGenerationService {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final WorkoutOccurrenceRepository occurrenceRepository;
	private final WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final Clock clock;

	DailyAthleteStateGenerationService(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			DailyRecoveryCheckInRepository checkInRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			WorkoutOccurrenceRepository occurrenceRepository,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
		this.occurrenceRepository = Objects.requireNonNull(occurrenceRepository);
		this.loadSummaryRepository = Objects.requireNonNull(loadSummaryRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	DailyAthleteStateSnapshotResult generate(
			AccountId accountId,
			LocalDate stateDate,
			int baselineWindowDays,
			DailyAthleteStateGenerationReason generationReason) {
		DailyAthleteStateSupport.requireStateDate(stateDate, clock);
		DailyAthleteStateSupport.requireBaselineWindow(baselineWindowDays);
		AthleteRef athlete = DailyAthleteStateSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		try {
			Optional<DailyAthleteStateSnapshot> lockedCurrent = snapshotRepository
					.findCurrentByAthleteIdAndStateDateForUpdate(athleteId, stateDate);

			AssembledDailyAthleteStateSource source = DailyAthleteStateSupport.assemble(
					athleteId,
					stateDate,
					baselineWindowDays,
					checkInRepository,
					trainingLoadQueryRepository,
					occurrenceRepository,
					loadSummaryRepository,
					sessionEffortRepository,
					clock);

			String fingerprint = DailyAthleteStateFingerprintCalculator.calculate(source.fingerprintInput());

			if (lockedCurrent.isPresent()) {
				DailyAthleteStateSnapshot current = lockedCurrent.get();
				if (current.sourceFingerprint().equals(fingerprint)
						&& current.baselineWindowDays() == baselineWindowDays
						&& current.recoveryAnalyticsCalculationVersion()
								== DailyAthleteStateSupport.ANALYTICS_VERSION) {
					return DailyAthleteStateSnapshotResult.from(current, false);
				}
				snapshotRepository.markNotCurrent(current.id(), athleteId);
			}

			int nextVersion = snapshotRepository.nextSnapshotVersion(athleteId, stateDate);
			DailyAthleteStateSnapshot created = DailyAthleteStateSnapshotFactory.create(
					athleteId,
					stateDate,
					nextVersion,
					baselineWindowDays,
					generationReason,
					source,
					clock);
			DailyAthleteStateSnapshot saved = snapshotRepository.saveNew(created);
			return DailyAthleteStateSnapshotResult.from(saved, true);
		}
		catch (DailyAthleteStateDateOutOfRangeException
				| InvalidDailyAthleteStateDateException
				| InvalidDailyAthleteStateBaselineWindowException
				| DailyAthleteStateVersionConflictException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new DailyAthleteStateGenerationFailedException(
					"Failed to generate daily athlete state snapshot", ex);
		}
	}

}
