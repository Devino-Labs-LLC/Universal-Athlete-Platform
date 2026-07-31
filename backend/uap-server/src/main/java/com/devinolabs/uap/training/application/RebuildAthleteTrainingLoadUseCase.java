package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

@Service
public class RebuildAthleteTrainingLoadUseCase {

	private final AthleteContextPort athleteContextPort;
	private final WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository;
	private final WorkoutLoadCalculationSupport loadCalculationSupport;

	public RebuildAthleteTrainingLoadUseCase(
			AthleteContextPort athleteContextPort,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository,
			WorkoutLoadCalculationSupport loadCalculationSupport) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.loadSummaryRepository = Objects.requireNonNull(loadSummaryRepository);
		this.loadCalculationSupport = Objects.requireNonNull(loadCalculationSupport);
	}

	@Transactional
	public TrainingLoadRebuildResult execute(AccountId accountId) {
		AthleteRef athlete = TrainingPerformanceSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		try {
			List<CompletedOccurrenceLoadRow> rows = loadSummaryRepository
					.findCompletedOccurrencesChronologically(athleteId);
			int created = 0;
			int updated = 0;
			int unchanged = 0;
			for (CompletedOccurrenceLoadRow row : rows) {
				WorkoutOccurrence occurrence = row.occurrence();
				WorkoutOccurrenceLoadSummary before = loadSummaryRepository
						.findByOccurrenceIdAndAthleteId(row.occurrenceId(), athleteId)
						.orElse(null);
				WorkoutOccurrenceLoadSummary after = loadCalculationSupport.calculateAndPersist(
						occurrence,
						athleteId,
						row.trainingPlanId(),
						row.workoutDayId(),
						null,
						occurrence.updatedAt());
				if (before == null) {
					created++;
				}
				else if (summariesEqual(before, after)) {
					unchanged++;
				}
				else {
					updated++;
				}
			}
			return new TrainingLoadRebuildResult(rows.size(), created, updated, unchanged);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new TrainingLoadRebuildConflictException(ex);
		}
		catch (RuntimeException ex) {
			if (ex instanceof TrainingLoadRebuildConflictException || ex instanceof WorkoutLoadCalculationFailedException) {
				throw ex;
			}
			throw new TrainingLoadRebuildFailedException(ex);
		}
	}

	private static boolean summariesEqual(WorkoutOccurrenceLoadSummary before, WorkoutOccurrenceLoadSummary after) {
		return before.prescribedExerciseCount() == after.prescribedExerciseCount()
				&& before.completedExerciseCount() == after.completedExerciseCount()
				&& before.substitutedExerciseCount() == after.substitutedExerciseCount()
				&& before.completedSetCount() == after.completedSetCount()
				&& before.skippedSetCount() == after.skippedSetCount()
				&& before.completedRepetitionCount() == after.completedRepetitionCount()
				&& before.totalVolumeKilograms().compareTo(after.totalVolumeKilograms()) == 0
				&& before.totalDurationSeconds() == after.totalDurationSeconds()
				&& before.totalDistanceMeters().compareTo(after.totalDistanceMeters()) == 0
				&& Objects.equals(before.sessionRpe(), after.sessionRpe())
				&& Objects.equals(before.sessionDurationMinutes(), after.sessionDurationMinutes())
				&& Objects.equals(
						before.sessionRpeLoad() == null ? null : before.sessionRpeLoad().value(),
						after.sessionRpeLoad() == null ? null : after.sessionRpeLoad().value());
	}
}
