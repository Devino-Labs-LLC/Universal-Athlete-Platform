package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.TrainingLoadQueryRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

@Repository
class JpaTrainingLoadQueryRepository implements TrainingLoadQueryRepository {

	private final WorkoutOccurrenceLoadSummaryJpaRepository jpaRepository;

	JpaTrainingLoadQueryRepository(WorkoutOccurrenceLoadSummaryJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public List<WorkoutOccurrenceLoadSummary> findOccurrenceSummaries(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern,
			int page,
			int size) {
		List<WorkoutOccurrenceLoadSummary> all = WorkoutOccurrenceLoadSummaryFetchSupport.loadFilteredWithChildren(
				jpaRepository,
				jpaRepository.findFiltered(
						athleteId.value(),
						startDate,
						endDate,
						uuidOrNull(trainingPlanId),
						uuidOrNull(workoutDayId),
						category,
						movementPattern))
				.stream()
				.map(WorkoutOccurrenceLoadSummaryPersistenceMapper::toDomain)
				.toList();
		int fromIndex = Math.min(page * size, all.size());
		int toIndex = Math.min(fromIndex + size, all.size());
		return all.subList(fromIndex, toIndex);
	}

	@Override
	public long countOccurrenceSummaries(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		return jpaRepository.countFiltered(
				athleteId.value(),
				startDate,
				endDate,
				uuidOrNull(trainingPlanId),
				uuidOrNull(workoutDayId),
				category,
				movementPattern);
	}

	@Override
	public List<DailyTrainingLoadSummary> aggregateDaily(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		List<WorkoutOccurrenceLoadSummary> summaries = loadSummaries(
				athleteId, startDate, endDate, trainingPlanId, workoutDayId, category, movementPattern);
		return TrainingLoadAggregationSupport.aggregateDaily(summaries, startDate, endDate, category, movementPattern);
	}

	@Override
	public List<WeeklyTrainingLoadSummary> aggregateWeekly(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		List<WorkoutOccurrenceLoadSummary> summaries = loadSummaries(
				athleteId, startDate, endDate, trainingPlanId, workoutDayId, category, movementPattern);
		return TrainingLoadAggregationSupport.aggregateWeekly(
				summaries, startDate, endDate, category, movementPattern);
	}

	private List<WorkoutOccurrenceLoadSummary> loadSummaries(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId trainingPlanId,
			WorkoutDayId workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern) {
		return WorkoutOccurrenceLoadSummaryFetchSupport.loadFilteredWithChildren(
				jpaRepository,
				jpaRepository.findFiltered(
						athleteId.value(),
						startDate,
						endDate,
						uuidOrNull(trainingPlanId),
						uuidOrNull(workoutDayId),
						category,
						movementPattern))
				.stream()
				.map(WorkoutOccurrenceLoadSummaryPersistenceMapper::toDomain)
				.toList();
	}

	private static UUID uuidOrNull(TrainingPlanId planId) {
		return planId == null ? null : planId.value();
	}

	private static UUID uuidOrNull(WorkoutDayId dayId) {
		return dayId == null ? null : dayId.value();
	}

}
