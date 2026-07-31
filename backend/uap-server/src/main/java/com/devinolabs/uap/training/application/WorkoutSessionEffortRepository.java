package com.devinolabs.uap.training.application;

import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;

public interface WorkoutSessionEffortRepository {

	WorkoutSessionEffort save(WorkoutSessionEffort effort);

	Optional<WorkoutSessionEffort> findByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	Optional<WorkoutSessionEffort> findByIdAndAthleteId(WorkoutSessionEffortId id, AthleteId athleteId);

	boolean existsByOccurrenceId(WorkoutOccurrenceId occurrenceId);

}
