package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
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

	void delete(WorkoutOccurrence occurrence);

}
