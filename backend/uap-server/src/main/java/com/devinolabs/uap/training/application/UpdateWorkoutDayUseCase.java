package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;

@Service
public class UpdateWorkoutDayUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final Clock clock;

	public UpdateWorkoutDayUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutDayResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			UpdateWorkoutDayCommand command) {
		AthleteRef athlete = WorkoutDaySupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = workoutDayRepository
				.findByIdAndTrainingPlanIdAndAthleteId(dayId, plan.id(), athleteId)
				.orElseThrow(WorkoutDayNotFoundException::new);

		if (command.titlePresent()) {
			if (command.title() == null || command.title().isBlank()) {
				throw new IllegalArgumentException("title must not be blank");
			}
			WorkoutDaySupport.assertUniqueTitle(workoutDayRepository, plan.id(), command.title(), day.id());
		}

		if (command.touchesPlacement()) {
			WorkoutDaySupport.assertPlacementUnlocked(workoutOccurrenceRepository, plan, day.id());
			Integer targetWeek = command.planWeekNumberPresent() ? command.planWeekNumber() : day.planWeekNumber();
			DayOfWeek targetDayOfWeek = command.scheduledDayOfWeekPresent()
					? command.scheduledDayOfWeek()
					: day.scheduledDayOfWeek();
			LocalTime targetStartTime = command.plannedStartTimePresent()
					? command.plannedStartTime()
					: day.plannedStartTime();
			WorkoutDaySupport.assertUniquePlacement(
					workoutDayRepository, plan.id(), targetWeek, targetDayOfWeek, targetStartTime, day.id());
		}

		try {
			if (command.titlePresent()) {
				day.rename(command.title(), clock);
			}
			if (command.descriptionPresent()) {
				day.changeDescription(command.description(), clock);
			}
			if (command.planWeekNumberPresent()) {
				day.changePlanWeekNumber(command.planWeekNumber(), clock);
			}
			if (command.scheduledDayOfWeekPresent()) {
				if (command.scheduledDayOfWeek() == null) {
					throw new IllegalArgumentException("scheduledDayOfWeek must not be null");
				}
				day.changeScheduledDayOfWeek(command.scheduledDayOfWeek(), clock);
			}
			if (command.plannedStartTimePresent()) {
				day.changePlannedStartTime(command.plannedStartTime(), clock);
			}
			if (command.expectedDurationMinutesPresent()) {
				day.changeExpectedDurationMinutes(command.expectedDurationMinutes(), clock);
			}
		}
		catch (IllegalArgumentException ex) {
			throw WorkoutDaySupport.translateValidation(ex);
		}

		WorkoutDay saved = workoutDayRepository.save(day);

		if (command.displayOrderPresent()) {
			if (command.displayOrder() == null) {
				throw new InvalidWorkoutDayOrderException("displayOrder cannot be null");
			}
			if (command.displayOrder() < 0) {
				throw new InvalidWorkoutDayOrderException("displayOrder must not be negative");
			}
			WorkoutDayId savedId = saved.id();
			List<WorkoutDay> all = new ArrayList<>(
					workoutDayRepository.findAllByTrainingPlanIdAndAthleteId(plan.id(), athleteId));
			WorkoutDay current = all.stream()
					.filter(existing -> existing.id().equals(savedId))
					.findFirst()
					.orElseThrow(WorkoutDayNotFoundException::new);
			all.removeIf(existing -> existing.id().equals(savedId));
			int target = Math.min(command.displayOrder(), all.size());
			all.add(target, current);
			WorkoutDaySupport.reassignOrders(all, workoutDayRepository, plan.id(), athleteId, clock);
			saved = workoutDayRepository
					.findByIdAndTrainingPlanIdAndAthleteId(dayId, plan.id(), athleteId)
					.orElseThrow(WorkoutDayNotFoundException::new);
		}

		return WorkoutDaySupport.toResult(saved);
	}

}
