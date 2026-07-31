package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

public interface WorkoutOccurrenceLoadSummaryRepository {

	WorkoutOccurrenceLoadSummary save(WorkoutOccurrenceLoadSummary summary);

	Optional<WorkoutOccurrenceLoadSummary> findByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId);

	void deleteAllByAthleteId(AthleteId athleteId);

	List<CompletedOccurrenceLoadRow> findCompletedOccurrencesChronologically(AthleteId athleteId);

}
