package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
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
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutExercise;

@Service
public class ActivateTrainingPlanScheduleUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final WorkoutOccurrenceGenerator workoutOccurrenceGenerator;
	private final Clock clock;

	public ActivateTrainingPlanScheduleUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			WorkoutOccurrenceGenerator workoutOccurrenceGenerator,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.workoutOccurrenceGenerator = Objects.requireNonNull(workoutOccurrenceGenerator);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingPlanScheduleActivationResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			ActivateTrainingPlanScheduleCommand command) {
		AthleteRef athlete = TrainingPlanSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);

		TrainingScheduleSupport.requireZone(command.timezone());
		LocalDate scheduleStartDate = command.scheduleStartDate();
		if (scheduleStartDate == null) {
			throw new InvalidTrainingPlanScheduleDatesException("scheduleStartDate is required");
		}
		if (command.scheduleEndDate() != null && command.scheduleEndDate().isBefore(scheduleStartDate)) {
			throw new InvalidTrainingPlanScheduleDatesException(
					"scheduleEndDate must not be before scheduleStartDate");
		}
		TrainingPlanRecurrenceMode recurrenceMode = command.recurrenceMode();
		if (recurrenceMode == null) {
			throw new IllegalArgumentException("recurrenceMode is required to activate a schedule");
		}

		List<WorkoutDay> days = workoutDayRepository
				.findAllByTrainingPlanIdAndAthleteIdOrderedByPlacement(plan.id(), athleteId);
		requireSchedulableDays(days, athleteId);

		if (recurrenceMode == TrainingPlanRecurrenceMode.FINITE && command.scheduleEndDate() != null) {
			LocalDate lastPlacement = TrainingScheduleSupport.finiteLastPlacementDate(scheduleStartDate, days);
			if (command.scheduleEndDate().isBefore(lastPlacement)) {
				throw new InvalidTrainingPlanScheduleDatesException(
						"scheduleEndDate must not be before the final planned workout date " + lastPlacement);
			}
		}

		try {
			plan.activateSchedule(
					scheduleStartDate,
					command.scheduleEndDate(),
					command.timezone(),
					recurrenceMode,
					clock);
		}
		catch (IllegalStateException ex) {
			throw TrainingScheduleSupport.translateScheduleState(ex);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingPlanScheduleDatesException(ex.getMessage());
		}

		if (command.generateThrough() == null) {
			TrainingPlan saved = trainingPlanRepository.save(plan);
			return new TrainingPlanScheduleActivationResult(TrainingPlanSupport.toResult(saved), null);
		}

		// The generator persists the plan itself when it advances the watermark; saving here as well
		// would flush an intermediate version and make the generator's write look stale.
		WorkoutOccurrenceGenerationResult generation = workoutOccurrenceGenerator.generate(
				plan, athleteId, scheduleStartDate, command.generateThrough());
		TrainingPlan reloaded = WorkoutDaySupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		return new TrainingPlanScheduleActivationResult(TrainingPlanSupport.toResult(reloaded), generation);
	}

	private void requireSchedulableDays(List<WorkoutDay> days, AthleteId athleteId) {
		if (days.isEmpty()) {
			throw new TrainingPlanScheduleRequiresWorkoutDaysException(
					"Training plan must have at least one workout day before activation");
		}
		for (WorkoutDay day : days) {
			if (!day.hasSchedulablePlacement()) {
				throw new TrainingPlanScheduleRequiresWorkoutDaysException(
						"Workout day '" + day.title() + "' is missing planWeekNumber or scheduledDayOfWeek");
			}
			List<WorkoutExercise> exercises = workoutExerciseRepository
					.findAllByWorkoutDayIdAndAthleteId(day.id(), athleteId);
			if (exercises.isEmpty()) {
				throw new WorkoutOccurrenceRequiresExercisesException();
			}
		}
	}

}
