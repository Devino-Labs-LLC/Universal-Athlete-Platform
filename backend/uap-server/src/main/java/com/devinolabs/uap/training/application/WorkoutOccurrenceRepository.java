package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutGenerationKey;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

public interface WorkoutOccurrenceRepository {

	WorkoutOccurrence save(WorkoutOccurrence occurrence);

	Optional<WorkoutOccurrence> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutOccurrenceId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId);

	List<WorkoutOccurrence> findAllByWorkoutDayIdAndAthleteId(
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			WorkoutOccurrenceStatus status,
			LocalDate scheduledFrom,
			LocalDate scheduledTo);

	boolean existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			WorkoutOccurrenceStatus excludedStatus);

	Optional<WorkoutOccurrence> findByGenerationKey(WorkoutGenerationKey generationKey);

	boolean existsByGenerationKey(WorkoutGenerationKey generationKey);

	boolean existsByWorkoutDayIdAndOrigin(WorkoutDayId workoutDayId, WorkoutOccurrenceOrigin origin);

	/**
	 * Calendar feed ordered by scheduled date, then planned start time with nulls last,
	 * then creation time.
	 */
	List<WorkoutOccurrence> findCalendarRange(
			AthleteId athleteId,
			LocalDate from,
			LocalDate to,
			WorkoutOccurrenceStatus status,
			TrainingPlanId trainingPlanId);

	void delete(WorkoutOccurrence occurrence);

}
