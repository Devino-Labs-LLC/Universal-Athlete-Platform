package com.devinolabs.uap.training.application;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;

public interface WorkoutDayRepository {

	WorkoutDay save(WorkoutDay day);

	List<WorkoutDay> saveAll(Collection<WorkoutDay> days);

	Optional<WorkoutDay> findByIdAndTrainingPlanIdAndAthleteId(
			WorkoutDayId id,
			TrainingPlanId trainingPlanId,
			AthleteId athleteId);

	List<WorkoutDay> findAllByTrainingPlanIdAndAthleteId(TrainingPlanId trainingPlanId, AthleteId athleteId);

	/** Ordered by plan week, then weekday, then planned start time (nulls first), then display order. */
	List<WorkoutDay> findAllByTrainingPlanIdAndAthleteIdOrderedByPlacement(
			TrainingPlanId trainingPlanId,
			AthleteId athleteId);

	List<WorkoutDay> findAllByIdInAndAthleteId(Collection<WorkoutDayId> ids, AthleteId athleteId);

	boolean existsDuplicatePlacement(
			TrainingPlanId trainingPlanId,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			WorkoutDayId excludingId);

	boolean existsByTrainingPlanIdAndNormalizedTitle(TrainingPlanId trainingPlanId, String normalizedTitle);

	boolean existsByTrainingPlanIdAndNormalizedTitleExcluding(
			TrainingPlanId trainingPlanId,
			String normalizedTitle,
			WorkoutDayId excludingId);

	int findMaxDisplayOrder(TrainingPlanId trainingPlanId, AthleteId athleteId);

	List<WorkoutDay> findAllByTrainingPlanIdWithDisplayOrderAtLeast(
			TrainingPlanId trainingPlanId,
			int displayOrder);

	void delete(WorkoutDay day);

}
