package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Append-only store: entries are written once and only ever read back in chronological order.
 */
public interface WorkoutExerciseSubstitutionHistoryRepository {

	WorkoutExerciseSubstitutionHistory append(WorkoutExerciseSubstitutionHistory entry);

	List<WorkoutExerciseSubstitutionHistory> findAllByExecutionIdAndAthleteId(
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId);

	List<WorkoutExerciseSubstitutionHistory> findAllByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

}
