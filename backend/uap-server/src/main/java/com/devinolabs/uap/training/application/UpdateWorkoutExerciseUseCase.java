package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
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
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@Service
public class UpdateWorkoutExerciseUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final Clock clock;

	public UpdateWorkoutExerciseUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId,
			UpdateWorkoutExerciseCommand command) {
		AthleteRef athlete = WorkoutExerciseSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutExercise exercise = workoutExerciseRepository
				.findByIdAndWorkoutDayIdAndAthleteId(exerciseId, day.id(), athleteId)
				.orElseThrow(WorkoutExerciseNotFoundException::new);

		if (command.exerciseNamePresent()) {
			if (command.exerciseName() == null || command.exerciseName().isBlank()) {
				throw new IllegalArgumentException("exerciseName must not be blank");
			}
			WorkoutExerciseSupport.assertUniqueName(
					workoutExerciseRepository, day.id(), command.exerciseName(), exercise.id());
		}

		try {
			if (command.exerciseNamePresent()) {
				exercise.rename(command.exerciseName(), clock);
			}
			if (command.categoryPresent()) {
				if (command.category() == null) {
					throw new IllegalArgumentException("category must not be null");
				}
				exercise.changeCategory(command.category(), clock);
			}
			if (command.typePresent()) {
				if (command.type() == null) {
					throw new IllegalArgumentException("type must not be null");
				}
				exercise.changeType(command.type(), clock);
			}
			if (command.setsPresent()) {
				exercise.changeSets(command.sets(), clock);
			}

			boolean prescriptionTouched = command.minimumRepsPresent()
					|| command.maximumRepsPresent()
					|| command.targetWeightPresent()
					|| command.weightUnitPresent()
					|| command.targetDurationSecondsPresent()
					|| command.targetDistancePresent()
					|| command.distanceUnitPresent()
					|| command.targetRestSecondsPresent()
					|| command.targetRpePresent()
					|| command.tempoPresent()
					|| command.coachingNotesPresent();
			if (prescriptionTouched) {
				Integer minimumReps = command.minimumRepsPresent() ? command.minimumReps() : exercise.minimumReps();
				Integer maximumReps = command.maximumRepsPresent() ? command.maximumReps() : exercise.maximumReps();
				BigDecimal targetWeight = command.targetWeightPresent()
						? command.targetWeight()
						: exercise.targetWeight();
				WeightUnit weightUnit = command.weightUnitPresent() ? command.weightUnit() : exercise.weightUnit();
				Integer targetDurationSeconds = command.targetDurationSecondsPresent()
						? command.targetDurationSeconds()
						: exercise.targetDurationSeconds();
				BigDecimal targetDistance = command.targetDistancePresent()
						? command.targetDistance()
						: exercise.targetDistance();
				DistanceUnit distanceUnit = command.distanceUnitPresent()
						? command.distanceUnit()
						: exercise.distanceUnit();
				Integer targetRestSeconds = command.targetRestSecondsPresent()
						? command.targetRestSeconds()
						: exercise.targetRestSeconds();
				Integer targetRpe = command.targetRpePresent() ? command.targetRpe() : exercise.targetRpe();
				String tempo = command.tempoPresent() ? command.tempo() : exercise.tempo();
				String coachingNotes = command.coachingNotesPresent()
						? command.coachingNotes()
						: exercise.coachingNotes();
				exercise.changePrescription(
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
			}
		}
		catch (IllegalArgumentException ex) {
			throw WorkoutExerciseSupport.translateValidation(ex);
		}

		WorkoutExercise saved = workoutExerciseRepository.save(exercise);

		if (command.displayOrderPresent()) {
			if (command.displayOrder() == null) {
				throw new InvalidWorkoutExerciseOrderException("displayOrder cannot be null");
			}
			if (command.displayOrder() < 0) {
				throw new InvalidWorkoutExerciseOrderException("displayOrder must not be negative");
			}
			WorkoutExerciseId savedId = saved.id();
			List<WorkoutExercise> all = new ArrayList<>(
					workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId));
			WorkoutExercise current = all.stream()
					.filter(existing -> existing.id().equals(savedId))
					.findFirst()
					.orElseThrow(WorkoutExerciseNotFoundException::new);
			all.removeIf(existing -> existing.id().equals(savedId));
			int target = Math.min(command.displayOrder(), all.size());
			all.add(target, current);
			WorkoutExerciseSupport.reassignOrders(all, workoutExerciseRepository, day.id(), athleteId, clock);
			saved = workoutExerciseRepository
					.findByIdAndWorkoutDayIdAndAthleteId(exerciseId, day.id(), athleteId)
					.orElseThrow(WorkoutExerciseNotFoundException::new);
		}

		return WorkoutExerciseSupport.toResult(saved);
	}

}
