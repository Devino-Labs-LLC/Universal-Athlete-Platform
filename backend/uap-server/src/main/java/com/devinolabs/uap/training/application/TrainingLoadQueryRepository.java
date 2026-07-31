package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

public interface TrainingLoadQueryRepository {

	List<WorkoutOccurrenceLoadSummary> findOccurrenceSummaries(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern,
			int page,
			int size);

	long countOccurrenceSummaries(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern);

	List<DailyTrainingLoadSummary> aggregateDaily(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern);

	List<WeeklyTrainingLoadSummary> aggregateWeekly(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern);

}
