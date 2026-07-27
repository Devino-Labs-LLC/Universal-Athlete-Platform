package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordType;

public interface AthleteExercisePersonalRecordRepository {

	AthleteExercisePersonalRecord save(AthleteExercisePersonalRecord record);

	/**
	 * Current projections for one exercise, ordered by record type then qualifier.
	 */
	List<AthleteExercisePersonalRecord> findAllByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey);

	/**
	 * Current projections across all of the athlete's exercises, optionally filtered.
	 */
	List<AthleteExercisePersonalRecord> findAllByAthleteId(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType);

	/**
	 * Current projections whose achievement falls inside a recency window, best-dated first.
	 */
	List<AthleteExercisePersonalRecord> findRecentByAthleteId(
			AthleteId athleteId,
			Instant achievedFrom,
			int limit);

	/**
	 * Drops the athlete's current projections so a rebuild can replace them wholesale. A null key
	 * clears every exercise.
	 */
	void deleteAllByAthleteId(AthleteId athleteId, ExercisePerformanceKey exercisePerformanceKey);

}
