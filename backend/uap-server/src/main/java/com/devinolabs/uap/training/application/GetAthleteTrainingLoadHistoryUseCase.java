package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.TrainingLoadGranularity;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeeklyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.WorkoutDayId;

@Service
public class GetAthleteTrainingLoadHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;

	public GetAthleteTrainingLoadHistoryUseCase(
			AthleteContextPort athleteContextPort,
			TrainingLoadQueryRepository trainingLoadQueryRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
	}

	@Transactional(readOnly = true)
	public TrainingLoadHistoryResult execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			TrainingLoadGranularity granularity,
			UUID trainingPlanId,
			UUID workoutDayId,
			ExerciseDefinitionCategory category,
			MovementPattern movementPattern,
			Integer page,
			Integer size) {
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingLoadSupport.requireDateRange(startDate, endDate);
		TrainingLoadGranularity resolvedGranularity = TrainingLoadSupport.requireGranularity(granularity);
		TrainingPlanId planFilter = trainingPlanId == null ? null : TrainingPlanId.of(trainingPlanId);
		WorkoutDayId dayFilter = workoutDayId == null ? null : WorkoutDayId.of(workoutDayId);
		int resolvedPage = TrainingLoadSupport.requirePage(page);
		int resolvedSize = TrainingLoadSupport.requireSize(size);

		return switch (resolvedGranularity) {
		case OCCURRENCE -> {
			long total = trainingLoadQueryRepository.countOccurrenceSummaries(
					athleteId, startDate, endDate, planFilter, dayFilter, category, movementPattern);
			int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / resolvedSize);
			yield new TrainingLoadHistoryResult(
					resolvedGranularity,
					trainingLoadQueryRepository
							.findOccurrenceSummaries(
									athleteId,
									startDate,
									endDate,
									planFilter,
									dayFilter,
									category,
									movementPattern,
									resolvedPage,
									resolvedSize)
						.stream()
						.map(WorkoutOccurrenceLoadSummaryResult::from)
						.toList(),
					List.of(),
					List.of(),
					resolvedPage,
					resolvedSize,
					total,
					totalPages);
		}
		case DAILY -> new TrainingLoadHistoryResult(
				resolvedGranularity,
				List.of(),
				trainingLoadQueryRepository.aggregateDaily(
						athleteId, startDate, endDate, planFilter, dayFilter, category, movementPattern),
				List.of(),
				0,
				0,
				0,
				0);
		case WEEKLY -> new TrainingLoadHistoryResult(
				resolvedGranularity,
				List.of(),
				List.of(),
				trainingLoadQueryRepository.aggregateWeekly(
						athleteId, startDate, endDate, planFilter, dayFilter, category, movementPattern),
				0,
				0,
				0,
				0);
		};
	}
}
