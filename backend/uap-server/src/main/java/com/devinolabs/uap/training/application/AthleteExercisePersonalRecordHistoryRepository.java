package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistory;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordType;

public interface AthleteExercisePersonalRecordHistoryRepository {

	AthleteExercisePersonalRecordHistory save(AthleteExercisePersonalRecordHistory entry);

	/**
	 * The standing (not yet superseded) entry of one record slot.
	 */
	Optional<AthleteExercisePersonalRecordHistory> findCurrentForSlot(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType,
			String recordQualifier);

	/**
	 * Every entry for one exercise, oldest achievement first.
	 */
	List<AthleteExercisePersonalRecordHistory> findAllByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey);

	void deleteAllByAthleteId(AthleteId athleteId, ExercisePerformanceKey exercisePerformanceKey);

}
