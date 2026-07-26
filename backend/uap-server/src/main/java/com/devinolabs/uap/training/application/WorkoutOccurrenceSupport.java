package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

final class WorkoutOccurrenceSupport {

	private WorkoutOccurrenceSupport() {
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

	static WorkoutOccurrence requireOwnedOccurrence(
			WorkoutOccurrenceRepository repository,
			WorkoutOccurrenceId occurrenceId,
			WorkoutDayId dayId,
			AthleteId athleteId) {
		return repository
				.findByIdAndWorkoutDayIdAndAthleteId(occurrenceId, dayId, athleteId)
				.orElseThrow(WorkoutOccurrenceNotFoundException::new);
	}

	static void assertUniqueActiveDate(
			WorkoutOccurrenceRepository repository,
			WorkoutDayId dayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			WorkoutOccurrenceId excludingId) {
		if (repository.existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
				dayId, athleteId, scheduledDate, WorkoutOccurrenceStatus.CANCELLED)) {
			if (excludingId == null) {
				throw new DuplicateWorkoutOccurrenceException();
			}
			WorkoutOccurrence existing = repository
					.findAllByWorkoutDayIdAndAthleteId(dayId, athleteId, null, scheduledDate, scheduledDate)
					.stream()
					.filter(o -> o.status() != WorkoutOccurrenceStatus.CANCELLED)
					.filter(o -> !o.id().equals(excludingId))
					.findFirst()
					.orElse(null);
			if (existing != null) {
				throw new DuplicateWorkoutOccurrenceException();
			}
		}
	}

	static WorkoutOccurrenceResult toResult(WorkoutOccurrence occurrence) {
		return new WorkoutOccurrenceResult(
				occurrence.id(),
				occurrence.workoutDayId(),
				occurrence.scheduledDate(),
				occurrence.plannedStartTime(),
				occurrence.startedAt(),
				occurrence.completedAt(),
				occurrence.status(),
				occurrence.athleteNotes(),
				occurrence.createdAt(),
				occurrence.updatedAt());
	}

	static List<WorkoutOccurrenceResult> toResults(List<WorkoutOccurrence> occurrences) {
		return occurrences.stream().map(WorkoutOccurrenceSupport::toResult).toList();
	}

	static WorkoutOccurrenceDetailResult toDetailResult(
			WorkoutOccurrence occurrence,
			List<WorkoutExerciseExecutionResult> executions) {
		return new WorkoutOccurrenceDetailResult(toResult(occurrence), executions);
	}

	static RuntimeException translateStatus(IllegalStateException ex) {
		return new InvalidWorkoutOccurrenceStatusException(
				Objects.requireNonNullElse(ex.getMessage(), "Invalid workout occurrence status transition"));
	}

}
