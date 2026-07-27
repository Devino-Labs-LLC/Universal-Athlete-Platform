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
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@Service
public class CreateWorkoutExerciseUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final Clock clock;

	public CreateWorkoutExerciseUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			ExerciseDefinitionId exerciseDefinitionId,
			String exerciseName,
			ExerciseCategory category,
			ExerciseType type,
			Integer sets,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal targetWeight,
			WeightUnit weightUnit,
			Integer targetDurationSeconds,
			BigDecimal targetDistance,
			DistanceUnit distanceUnit,
			Integer targetRestSeconds,
			Integer targetRpe,
			String tempo,
			String coachingNotes,
			Integer displayOrder) {
		AthleteRef athlete = WorkoutExerciseSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		ExerciseDefinition definition = WorkoutExerciseSupport.requireSelectableDefinition(
				exerciseDefinitionRepository, athleteId, exerciseDefinitionId);

		String displayName = exerciseName == null || exerciseName.isBlank()
				? definition.canonicalName()
				: exerciseName;
		WorkoutExerciseSupport.assertUniqueName(workoutExerciseRepository, day.id(), displayName, null);

		int order;
		int max = workoutExerciseRepository.findMaxDisplayOrder(day.id(), athleteId);
		int size = max + 1;
		if (displayOrder == null) {
			order = size;
		}
		else {
			if (displayOrder < 0) {
				throw new InvalidWorkoutExerciseOrderException("displayOrder must not be negative");
			}
			order = Math.min(displayOrder, size);
			if (order < size) {
				WorkoutExerciseSupport.shiftOrdersUpFrom(workoutExerciseRepository, day.id(), order, clock);
			}
		}

		try {
			WorkoutExercise exercise = WorkoutExercise.create(
					WorkoutExerciseId.generate(),
					day.id(),
					athleteId,
					definition.id(),
					order,
					displayName,
					category,
					type,
					sets,
					minimumReps,
					maximumReps,
					targetWeight,
					weightUnit,
					targetDurationSeconds,
					targetDistance,
					distanceUnit,
					targetRestSeconds,
					targetRpe,
					tempo,
					coachingNotes,
					clock);
			return WorkoutExerciseSupport.toResult(workoutExerciseRepository.save(exercise));
		}
		catch (IllegalArgumentException ex) {
			throw WorkoutExerciseSupport.translateValidation(ex);
		}
	}

}
