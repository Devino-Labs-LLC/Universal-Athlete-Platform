package com.devinolabs.uap.training.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public interface WorkoutExerciseExecutionRepository {

	WorkoutExerciseExecution save(WorkoutExerciseExecution execution);

	List<WorkoutExerciseExecution> saveAll(Collection<WorkoutExerciseExecution> executions);

	Optional<WorkoutExerciseExecution> findByIdAndWorkoutOccurrenceIdAndAthleteId(
			WorkoutExerciseExecutionId id,
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	List<WorkoutExerciseExecution> findAllByWorkoutOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	Optional<WorkoutExerciseExecution> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseExecutionId id,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	List<WorkoutExerciseExecutionStatusCount> countByStatusForOccurrences(
			Collection<WorkoutOccurrenceId> occurrenceIds,
			AthleteId athleteId);

}
