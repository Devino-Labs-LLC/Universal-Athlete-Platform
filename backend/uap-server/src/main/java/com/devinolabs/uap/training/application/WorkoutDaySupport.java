package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
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
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;

final class WorkoutDaySupport {

	private WorkoutDaySupport() {
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
		TrainingPlan plan = trainingPlanRepository.findByIdAndAthleteId(planId, athleteId)
				.orElseThrow(TrainingPlanNotFoundException::new);
		if (plan.status() == TrainingPlanStatus.ARCHIVED) {
			throw new TrainingPlanArchivedException();
		}
		return plan;
	}

	static TrainingPlan requirePlan(
			TrainingPlanRepository trainingPlanRepository,
			AthleteId athleteId,
			TrainingPlanId planId) {
		return trainingPlanRepository.findByIdAndAthleteId(planId, athleteId)
				.orElseThrow(TrainingPlanNotFoundException::new);
	}

	static void assertUniqueTitle(
			WorkoutDayRepository repository,
			TrainingPlanId planId,
			String title,
			WorkoutDayId excludingId) {
		String normalized = WorkoutDay.normalizeTitle(title);
		boolean exists = excludingId == null
				? repository.existsByTrainingPlanIdAndNormalizedTitle(planId, normalized)
				: repository.existsByTrainingPlanIdAndNormalizedTitleExcluding(planId, normalized, excludingId);
		if (exists) {
			throw new DuplicateWorkoutDayException();
		}
	}

	static void assertUniquePlacement(
			WorkoutDayRepository repository,
			TrainingPlanId planId,
			Integer planWeekNumber,
			DayOfWeek scheduledDayOfWeek,
			LocalTime plannedStartTime,
			WorkoutDayId excludingId) {
		if (planWeekNumber == null || scheduledDayOfWeek == null) {
			return;
		}
		if (repository.existsDuplicatePlacement(
				planId, planWeekNumber, scheduledDayOfWeek, plannedStartTime, excludingId)) {
			throw new DuplicateWorkoutDayPlacementException();
		}
	}

	/**
	 * Placement fields feed deterministic generation keys, so they are frozen once an ACTIVE
	 * schedule has materialised occurrences for the day.
	 */
	static void assertPlacementUnlocked(
			WorkoutOccurrenceRepository occurrenceRepository,
			TrainingPlan plan,
			WorkoutDayId dayId) {
		if (!plan.isScheduleActive()) {
			return;
		}
		if (occurrenceRepository.existsByWorkoutDayIdAndOrigin(dayId, WorkoutOccurrenceOrigin.GENERATED)) {
			throw new TrainingPlanSchedulePlacementLockedException();
		}
	}

	static void shiftOrdersUpFrom(
			WorkoutDayRepository repository,
			TrainingPlanId planId,
			int fromOrder,
			Clock clock) {
		List<WorkoutDay> toShift = repository.findAllByTrainingPlanIdWithDisplayOrderAtLeast(planId, fromOrder);
		for (WorkoutDay day : toShift) {
			day.changeDisplayOrder(day.displayOrder() + 1, clock);
		}
		if (!toShift.isEmpty()) {
			repository.saveAll(toShift);
		}
	}

	static void compactOrders(
			List<WorkoutDay> days,
			WorkoutDayRepository repository,
			TrainingPlanId planId,
			AthleteId athleteId,
			Clock clock) {
		reassignOrders(days.stream()
				.sorted(Comparator
						.comparingInt(WorkoutDay::displayOrder)
						.thenComparing(WorkoutDay::createdAt)
						.thenComparing(day -> day.id().value()))
				.toList(), repository, planId, athleteId, clock);
	}

	static void reassignOrders(
			List<WorkoutDay> orderedDays,
			WorkoutDayRepository repository,
			TrainingPlanId planId,
			AthleteId athleteId,
			Clock clock) {
		if (orderedDays.isEmpty()) {
			return;
		}
		List<WorkoutDayId> orderedIds = orderedDays.stream().map(WorkoutDay::id).toList();
		List<WorkoutDay> mutable = new ArrayList<>(orderedDays);
		int offset = mutable.size() + 1000;
		for (int i = 0; i < mutable.size(); i++) {
			mutable.get(i).changeDisplayOrder(offset + i, clock);
		}
		repository.saveAll(mutable);
		// Reload so @Version reflects the flushed first-phase updates.
		Map<WorkoutDayId, WorkoutDay> refreshed = repository
				.findAllByTrainingPlanIdAndAthleteId(planId, athleteId)
				.stream()
				.collect(Collectors.toMap(WorkoutDay::id, Function.identity()));
		List<WorkoutDay> finalOrder = orderedIds.stream().map(refreshed::get).toList();
		for (int i = 0; i < finalOrder.size(); i++) {
			finalOrder.get(i).changeDisplayOrder(i, clock);
		}
		repository.saveAll(finalOrder);
	}

	static WorkoutDayResult toResult(WorkoutDay day) {
		return new WorkoutDayResult(
				day.id(),
				day.displayOrder(),
				day.title(),
				day.description(),
				day.planWeekNumber(),
				day.scheduledDayOfWeek(),
				day.plannedStartTime(),
				day.expectedDurationMinutes(),
				day.status(),
				day.createdAt(),
				day.updatedAt());
	}

	static List<WorkoutDayResult> toResults(List<WorkoutDay> days) {
		return days.stream().map(WorkoutDaySupport::toResult).toList();
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("displayOrder")) {
			return new InvalidWorkoutDayOrderException(message);
		}
		return ex;
	}

}
