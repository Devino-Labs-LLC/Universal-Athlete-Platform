package com.devinolabs.uap.training.application;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public interface WorkoutExerciseSetRepository {

	WorkoutExerciseSet save(WorkoutExerciseSet set);

	List<WorkoutExerciseSet> saveAll(Collection<WorkoutExerciseSet> sets);

	void delete(WorkoutExerciseSet set);

	Optional<WorkoutExerciseSet> findByIdAndExecutionIdAndAthleteId(
			WorkoutExerciseSetId id,
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId);

	List<WorkoutExerciseSet> findAllByExecutionIdAndAthleteId(
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId);

	List<WorkoutExerciseSet> findAllByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	List<WorkoutExerciseSetStatusCount> countByStatusForExecutions(
			Collection<WorkoutExerciseExecutionId> executionIds,
			AthleteId athleteId);

}
