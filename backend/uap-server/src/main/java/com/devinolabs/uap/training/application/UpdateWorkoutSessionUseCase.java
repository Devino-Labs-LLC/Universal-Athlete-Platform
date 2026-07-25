package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutSession;

@Service
public class UpdateWorkoutSessionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final WorkoutSessionRepository workoutSessionRepository;
	private final Clock clock;

	public UpdateWorkoutSessionUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			WorkoutSessionRepository workoutSessionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.workoutSessionRepository = Objects.requireNonNull(workoutSessionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutSessionResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId,
			UpdateWorkoutSessionCommand command) {
		AthleteRef athlete = WorkoutSessionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutSessionSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutSessionSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutSessionSupport.requireOwnedExercise(workoutExerciseRepository, day.id(), athleteId, exerciseId);
		WorkoutSession session = WorkoutSessionSupport.requireSession(
				workoutSessionRepository, exerciseId, day.id(), athleteId);

		boolean executionTouched = command.actualSetsPresent()
				|| command.actualRepsPresent()
				|| command.actualWeightPresent()
				|| command.weightUnitPresent()
				|| command.actualDurationSecondsPresent()
				|| command.actualDistancePresent()
				|| command.distanceUnitPresent()
				|| command.actualRestSecondsPresent()
				|| command.actualRpePresent();
		if (executionTouched) {
			Integer actualSets = command.actualSetsPresent() ? command.actualSets() : session.actualSets();
			Integer actualReps = command.actualRepsPresent() ? command.actualReps() : session.actualReps();
			BigDecimal actualWeight = command.actualWeightPresent()
					? command.actualWeight()
					: session.actualWeight();
			WeightUnit weightUnit = command.weightUnitPresent() ? command.weightUnit() : session.weightUnit();
			Integer actualDurationSeconds = command.actualDurationSecondsPresent()
					? command.actualDurationSeconds()
					: session.actualDurationSeconds();
			BigDecimal actualDistance = command.actualDistancePresent()
					? command.actualDistance()
					: session.actualDistance();
			DistanceUnit distanceUnit = command.distanceUnitPresent()
					? command.distanceUnit()
					: session.distanceUnit();
			Integer actualRestSeconds = command.actualRestSecondsPresent()
					? command.actualRestSeconds()
					: session.actualRestSeconds();
			Integer actualRpe = command.actualRpePresent() ? command.actualRpe() : session.actualRpe();
			session.updateExecution(
					actualSets,
					actualReps,
					actualWeight,
					weightUnit,
					actualDurationSeconds,
					actualDistance,
					distanceUnit,
					actualRestSeconds,
					actualRpe,
					clock);
		}
		if (command.athleteNotesPresent()) {
			session.updateNotes(command.athleteNotes(), clock);
		}

		return WorkoutSessionSupport.toResult(workoutSessionRepository.save(session));
	}

}
