package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

final class WorkoutExerciseSupport {

	private WorkoutExerciseSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static TrainingPlan requireMutablePlan(
			TrainingPlanRepository trainingPlanRepository,
			AthleteId athleteId,
			TrainingPlanId planId) {
		return WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
	}

	static TrainingPlan requirePlan(
			TrainingPlanRepository trainingPlanRepository,
			AthleteId athleteId,
			TrainingPlanId planId) {
		return WorkoutDaySupport.requirePlan(trainingPlanRepository, athleteId, planId);
	}

	static WorkoutDay requireOwnedDay(
			WorkoutDayRepository workoutDayRepository,
			TrainingPlanId planId,
			AthleteId athleteId,
			WorkoutDayId dayId) {
		return workoutDayRepository
				.findByIdAndTrainingPlanIdAndAthleteId(dayId, planId, athleteId)
				.orElseThrow(WorkoutDayNotFoundException::new);
	}

	static void assertUniqueName(
			WorkoutExerciseRepository repository,
			WorkoutDayId dayId,
			String exerciseName,
			WorkoutExerciseId excludingId) {
		String normalized = WorkoutExercise.normalizeExerciseName(exerciseName);
		boolean exists = excludingId == null
				? repository.existsByWorkoutDayIdAndNormalizedExerciseName(dayId, normalized)
				: repository.existsByWorkoutDayIdAndNormalizedExerciseNameExcluding(dayId, normalized, excludingId);
		if (exists) {
			throw new DuplicateWorkoutExerciseException();
		}
	}

	static void shiftOrdersUpFrom(
			WorkoutExerciseRepository repository,
			WorkoutDayId dayId,
			int fromOrder,
			Clock clock) {
		List<WorkoutExercise> toShift = repository.findAllByWorkoutDayIdWithDisplayOrderAtLeast(dayId, fromOrder);
		for (WorkoutExercise exercise : toShift) {
			exercise.changeDisplayOrder(exercise.displayOrder() + 1, clock);
		}
		if (!toShift.isEmpty()) {
			repository.saveAll(toShift);
		}
	}

	static void compactOrders(
			List<WorkoutExercise> exercises,
			WorkoutExerciseRepository repository,
			WorkoutDayId dayId,
			AthleteId athleteId,
			Clock clock) {
		reassignOrders(exercises.stream()
				.sorted(Comparator
						.comparingInt(WorkoutExercise::displayOrder)
						.thenComparing(WorkoutExercise::createdAt)
						.thenComparing(exercise -> exercise.id().value()))
				.toList(), repository, dayId, athleteId, clock);
	}

	static void reassignOrders(
			List<WorkoutExercise> orderedExercises,
			WorkoutExerciseRepository repository,
			WorkoutDayId dayId,
			AthleteId athleteId,
			Clock clock) {
		if (orderedExercises.isEmpty()) {
			return;
		}
		List<WorkoutExerciseId> orderedIds = orderedExercises.stream().map(WorkoutExercise::id).toList();
		List<WorkoutExercise> mutable = new ArrayList<>(orderedExercises);
		int offset = mutable.size() + 1000;
		for (int i = 0; i < mutable.size(); i++) {
			mutable.get(i).changeDisplayOrder(offset + i, clock);
		}
		repository.saveAll(mutable);
		Map<WorkoutExerciseId, WorkoutExercise> refreshed = repository
				.findAllByWorkoutDayIdAndAthleteId(dayId, athleteId)
				.stream()
				.collect(Collectors.toMap(WorkoutExercise::id, Function.identity()));
		List<WorkoutExercise> finalOrder = orderedIds.stream().map(refreshed::get).toList();
		for (int i = 0; i < finalOrder.size(); i++) {
			finalOrder.get(i).changeDisplayOrder(i, clock);
		}
		repository.saveAll(finalOrder);
	}

	static WorkoutExerciseResult toResult(WorkoutExercise exercise) {
		return new WorkoutExerciseResult(
				exercise.id(),
				exercise.displayOrder(),
				exercise.exerciseName(),
				exercise.category(),
				exercise.type(),
				exercise.sets(),
				exercise.minimumReps(),
				exercise.maximumReps(),
				exercise.targetWeight(),
				exercise.weightUnit(),
				exercise.targetDurationSeconds(),
				exercise.targetDistance(),
				exercise.distanceUnit(),
				exercise.targetRestSeconds(),
				exercise.targetRpe(),
				exercise.tempo(),
				exercise.coachingNotes(),
				exercise.status(),
				exercise.createdAt(),
				exercise.updatedAt());
	}

	static List<WorkoutExerciseResult> toResults(List<WorkoutExercise> exercises) {
		return exercises.stream().map(WorkoutExerciseSupport::toResult).toList();
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("displayOrder")) {
			return new InvalidWorkoutExerciseOrderException(message);
		}
		return ex;
	}

}
