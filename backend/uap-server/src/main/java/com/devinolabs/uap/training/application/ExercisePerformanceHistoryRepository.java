package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;

/**
 * Read access to the completed executions that performance metrics and personal records are
 * derived from.
 *
 * <p>Only occurrences that are IN_PROGRESS or COMPLETED are eligible; skipped and cancelled
 * occurrences never contribute.
 */
public interface ExercisePerformanceHistoryRepository {

	/**
	 * Completed executions for one exercise, newest first by scheduled date then completion time.
	 */
	ExercisePerformanceExecutionPage findCompletedExecutions(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			LocalDate scheduledFrom,
			LocalDate scheduledTo,
			int page,
			int size);

	/**
	 * Every eligible execution in deterministic chronological order, which is the order a rebuild
	 * must replay them in. A null key replays the athlete's whole training log.
	 */
	List<ExercisePerformanceExecutionRow> findEligibleExecutionsChronologically(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey);

	boolean existsByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey);

}
