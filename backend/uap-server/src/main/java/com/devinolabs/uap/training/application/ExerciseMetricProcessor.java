package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistory;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetricCalculator;
import com.devinolabs.uap.training.domain.ExercisePerformanceMetrics;
import com.devinolabs.uap.training.domain.PersonalRecordCandidate;
import com.devinolabs.uap.training.domain.PersonalRecordEvaluator;
import com.devinolabs.uap.training.domain.PersonalRecordProvenance;
import com.devinolabs.uap.training.domain.PersonalRecordSlot;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Folds one completed execution's sets into the athlete's performance metrics and personal record
 * projections.
 *
 * <p>Callers run this inside their own transaction so a failure here rolls the whole completion
 * back rather than leaving an execution completed with stale records.
 */
@Service
public class ExerciseMetricProcessor {

	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final AthleteExercisePersonalRecordHistoryRepository personalRecordHistoryRepository;
	private final Clock clock;

	public ExerciseMetricProcessor(
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			AthleteExercisePersonalRecordHistoryRepository personalRecordHistoryRepository,
			Clock clock) {
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.personalRecordHistoryRepository = Objects.requireNonNull(personalRecordHistoryRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	/**
	 * Only occurrences the athlete actually trained count toward records; skipped and cancelled
	 * ones never do.
	 */
	public static boolean isEligibleOccurrence(WorkoutOccurrenceStatus status) {
		return status == WorkoutOccurrenceStatus.IN_PROGRESS || status == WorkoutOccurrenceStatus.COMPLETED;
	}

	public ExercisePerformanceMetrics process(
			AthleteId athleteId,
			WorkoutExerciseExecution execution,
			WorkoutOccurrenceStatus occurrenceStatus,
			LocalDate scheduledDate,
			List<WorkoutExerciseSet> sets) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(execution, "execution must not be null");
		Objects.requireNonNull(sets, "sets must not be null");
		if (execution.status() != WorkoutExerciseExecutionStatus.COMPLETED) {
			throw new TrainingMetricsRequireCompletedExecutionException();
		}
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(sets);
		if (!isEligibleOccurrence(occurrenceStatus)) {
			return metrics;
		}
		applyCandidates(
				athleteId,
				execution.exercisePerformanceKey(),
				ExercisePerformanceMetricCalculator.candidates(sets),
				new PersonalRecordProvenance(
						execution.exerciseName(),
						execution.id(),
						execution.workoutOccurrenceId(),
						scheduledDate));
		return metrics;
	}

	private void applyCandidates(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			List<PersonalRecordCandidate> candidates,
			PersonalRecordProvenance provenance) {
		if (candidates.isEmpty()) {
			return;
		}
		Map<PersonalRecordSlot, AthleteExercisePersonalRecord> current = new HashMap<>();
		for (AthleteExercisePersonalRecord record : personalRecordRepository
				.findAllByAthleteIdAndExercisePerformanceKey(athleteId, exercisePerformanceKey)) {
			current.put(record.slot(), record);
		}
		for (Map.Entry<PersonalRecordSlot, List<PersonalRecordCandidate>> entry
				: bySlot(candidates).entrySet()) {
			PersonalRecordCandidate best = PersonalRecordEvaluator.best(entry.getValue());
			apply(athleteId, exercisePerformanceKey, best, current.get(entry.getKey()), provenance);
		}
	}

	private void apply(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordCandidate candidate,
			AthleteExercisePersonalRecord existing,
			PersonalRecordProvenance provenance) {
		PersonalRecordEvaluator.Outcome outcome = PersonalRecordEvaluator.evaluate(candidate, existing);
		if (!outcome.writesProjection()) {
			return;
		}
		AthleteExercisePersonalRecord saved;
		if (existing == null) {
			saved = personalRecordRepository.save(AthleteExercisePersonalRecord.fromCandidate(
					athleteId, exercisePerformanceKey, candidate, provenance, clock));
		}
		else {
			existing.replaceWith(candidate, provenance, clock);
			saved = personalRecordRepository.save(existing);
		}
		if (!outcome.appendsHistory()) {
			return;
		}
		AthleteExercisePersonalRecordHistory appended = AthleteExercisePersonalRecordHistory.append(saved, clock);
		personalRecordHistoryRepository
				.findCurrentForSlot(
						athleteId, exercisePerformanceKey, candidate.recordType(), candidate.recordQualifier())
				.ifPresent(previous -> {
					previous.supersede(appended, clock);
					personalRecordHistoryRepository.save(previous);
				});
		personalRecordHistoryRepository.save(appended);
	}

	private static Map<PersonalRecordSlot, List<PersonalRecordCandidate>> bySlot(
			List<PersonalRecordCandidate> candidates) {
		Map<PersonalRecordSlot, List<PersonalRecordCandidate>> bySlot = new LinkedHashMap<>();
		for (PersonalRecordCandidate candidate : candidates) {
			bySlot.computeIfAbsent(candidate.slot(), slot -> new ArrayList<>()).add(candidate);
		}
		return bySlot;
	}

}
