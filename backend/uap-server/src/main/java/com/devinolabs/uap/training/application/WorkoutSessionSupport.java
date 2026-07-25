package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutSession;
import com.devinolabs.uap.training.domain.WorkoutSessionId;

final class WorkoutSessionSupport {

	private WorkoutSessionSupport() {
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
		return WorkoutExerciseSupport.requireOwnedDay(workoutDayRepository, planId, athleteId, dayId);
	}

	static WorkoutExercise requireOwnedExercise(
			WorkoutExerciseRepository workoutExerciseRepository,
			WorkoutDayId dayId,
			AthleteId athleteId,
			WorkoutExerciseId exerciseId) {
		return workoutExerciseRepository
				.findByIdAndWorkoutDayIdAndAthleteId(exerciseId, dayId, athleteId)
				.orElseThrow(WorkoutExerciseNotFoundException::new);
	}

	static WorkoutSession requireSession(
			WorkoutSessionRepository workoutSessionRepository,
			WorkoutExerciseId exerciseId,
			WorkoutDayId dayId,
			AthleteId athleteId) {
		return workoutSessionRepository
				.findByWorkoutExerciseIdAndWorkoutDayIdAndAthleteId(exerciseId, dayId, athleteId)
				.orElseThrow(WorkoutSessionNotFoundException::new);
	}

	static WorkoutSession getOrCreateSession(
			WorkoutSessionRepository workoutSessionRepository,
			WorkoutExercise exercise,
			AthleteId athleteId,
			Clock clock) {
		return workoutSessionRepository
				.findByWorkoutExerciseIdAndAthleteId(exercise.id(), athleteId)
				.orElseGet(() -> WorkoutSession.create(
						WorkoutSessionId.generate(),
						exercise.id(),
						exercise.workoutDayId(),
						athleteId,
						clock));
	}

	static WorkoutSessionResult toResult(WorkoutSession session) {
		return new WorkoutSessionResult(
				session.id(),
				session.status(),
				session.actualSets(),
				session.actualReps(),
				session.actualWeight(),
				session.weightUnit(),
				session.actualDurationSeconds(),
				session.actualDistance(),
				session.distanceUnit(),
				session.actualRestSeconds(),
				session.actualRpe(),
				session.completedAt(),
				session.athleteNotes(),
				session.createdAt(),
				session.updatedAt());
	}

	static RuntimeException translateStatus(IllegalStateException ex) {
		return new InvalidWorkoutSessionStatusException(
				Objects.requireNonNullElse(ex.getMessage(), "Invalid workout session status transition"));
	}

}
