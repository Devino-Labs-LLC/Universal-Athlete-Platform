package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class GetWorkoutOccurrenceLoadSummaryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository;

	public GetWorkoutOccurrenceLoadSummaryUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutOccurrenceLoadSummaryRepository loadSummaryRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.loadSummaryRepository = Objects.requireNonNull(loadSummaryRepository);
	}

	@Transactional(readOnly = true)
	public WorkoutOccurrenceLoadSummaryResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, planId, athleteId, dayId);
		WorkoutOccurrenceSupport.requireOwnedOccurrence(workoutOccurrenceRepository, occurrenceId, dayId, athleteId);
		return loadSummaryRepository.findByOccurrenceIdAndAthleteId(occurrenceId, athleteId)
				.map(WorkoutOccurrenceLoadSummaryResult::from)
				.orElseThrow(WorkoutLoadSummaryNotFoundException::new);
	}
}
