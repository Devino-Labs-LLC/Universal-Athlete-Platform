package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;

/**
 * Rebuilds an athlete's personal record projection and history from scratch.
 *
 * <p>Application-internal on purpose: there is no HTTP surface for it. The athlete row is taken
 * under a PESSIMISTIC_WRITE lock so no completion can interleave, the existing projection and
 * history are dropped, and every eligible execution is replayed in deterministic chronological
 * order. All of that happens in one transaction, so a failure leaves the previous projection in
 * place.
 */
@Service
public class RebuildAthletePersonalRecordsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final AthleteExercisePersonalRecordHistoryRepository personalRecordHistoryRepository;
	private final ExerciseMetricProcessor exerciseMetricProcessor;

	public RebuildAthletePersonalRecordsUseCase(
			AthleteContextPort athleteContextPort,
			ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			AthleteExercisePersonalRecordHistoryRepository personalRecordHistoryRepository,
			ExerciseMetricProcessor exerciseMetricProcessor) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exercisePerformanceHistoryRepository = Objects.requireNonNull(exercisePerformanceHistoryRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.personalRecordHistoryRepository = Objects.requireNonNull(personalRecordHistoryRepository);
		this.exerciseMetricProcessor = Objects.requireNonNull(exerciseMetricProcessor);
	}

	/**
	 * @param exercisePerformanceKey limits the rebuild to one exercise; null rebuilds everything
	 */
	@Transactional
	public PersonalRecordRebuildResult execute(AccountId accountId, ExercisePerformanceKey exercisePerformanceKey) {
		AthleteRef athlete = TrainingPerformanceSupport.requireMutableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		try {
			personalRecordHistoryRepository.deleteAllByAthleteId(athleteId, exercisePerformanceKey);
			personalRecordRepository.deleteAllByAthleteId(athleteId, exercisePerformanceKey);

			List<ExercisePerformanceExecutionRow> rows = exercisePerformanceHistoryRepository
					.findEligibleExecutionsChronologically(athleteId, exercisePerformanceKey);
			Map<WorkoutExerciseExecutionId, List<WorkoutExerciseSet>> sets =
					TrainingPerformanceSupport.setsByExecution(
							workoutExerciseSetRepository,
							rows.stream().map(row -> row.execution().id()).toList(),
							athleteId);

			for (ExercisePerformanceExecutionRow row : rows) {
				WorkoutExerciseExecution execution = row.execution();
				exerciseMetricProcessor.process(
						athleteId,
						execution,
						row.occurrenceStatus(),
						row.scheduledDate(),
						sets.getOrDefault(execution.id(), List.of()));
			}

			return new PersonalRecordRebuildResult(
					rows.size(),
					personalRecordRepository.findAllByAthleteId(athleteId, exercisePerformanceKey, null).size(),
					historyEntryCount(athleteId, exercisePerformanceKey, rows));
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new PersonalRecordRebuildConflictException(ex);
		}
	}

	private int historyEntryCount(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			List<ExercisePerformanceExecutionRow> rows) {
		return (exercisePerformanceKey == null
				? rows.stream().map(row -> row.execution().exercisePerformanceKey()).distinct().toList()
				: List.of(exercisePerformanceKey))
				.stream()
				.mapToInt(key -> personalRecordHistoryRepository
						.findAllByAthleteIdAndExercisePerformanceKey(athleteId, key)
						.size())
				.sum();
	}

}
