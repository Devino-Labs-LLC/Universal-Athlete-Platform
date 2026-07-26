package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingScheduleDateCalculator;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutGenerationKey;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Materialises schedule placements into dated occurrences.
 *
 * <p>Shared by {@link GenerateWorkoutOccurrencesUseCase} and by activation when the caller asks for
 * an initial horizon; callers own the transaction and the athlete row lock.
 */
@Component
class WorkoutOccurrenceGenerator {

	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutExerciseRepository workoutExerciseRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final TrainingPlanRepository trainingPlanRepository;
	private final Clock clock;

	WorkoutOccurrenceGenerator(
			WorkoutDayRepository workoutDayRepository,
			WorkoutExerciseRepository workoutExerciseRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			TrainingPlanRepository trainingPlanRepository,
			Clock clock) {
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutExerciseRepository = Objects.requireNonNull(workoutExerciseRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	static void requireValidRange(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new InvalidWorkoutOccurrenceGenerationRangeException("from and to are required");
		}
		if (to.isBefore(from)) {
			throw new InvalidWorkoutOccurrenceGenerationRangeException("to must not be before from");
		}
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > TrainingScheduleSupport.MAX_GENERATION_RANGE_DAYS) {
			throw new InvalidWorkoutOccurrenceGenerationRangeException(
					"Generation range must not exceed " + TrainingScheduleSupport.MAX_GENERATION_RANGE_DAYS
							+ " days");
		}
	}

	WorkoutOccurrenceGenerationResult generate(
			TrainingPlan plan,
			AthleteId athleteId,
			LocalDate from,
			LocalDate to) {
		requireValidRange(from, to);
		TrainingScheduleSupport.requireScheduleActive(plan);
		TrainingScheduleSupport.requireScheduleConfigured(plan);

		List<WorkoutDay> days = workoutDayRepository
				.findAllByTrainingPlanIdAndAthleteIdOrderedByPlacement(plan.id(), athleteId)
				.stream()
				.filter(WorkoutDay::hasSchedulablePlacement)
				.toList();
		if (days.isEmpty()) {
			throw new TrainingPlanScheduleRequiresWorkoutDaysException(
					"Training plan has no schedulable workout days");
		}

		List<Placement> placements = enumeratePlacements(plan, days, from, to);
		int outOfScheduleCount = (int) placements.stream().filter(Placement::outOfSchedule).count();

		Map<WorkoutDayId, List<WorkoutExercise>> exercisesByDay = new HashMap<>();
		List<WorkoutOccurrenceResult> created = new ArrayList<>();
		int existingCount = 0;
		int cancelledPlacementCount = 0;

		for (Placement placement : placements) {
			if (placement.outOfSchedule()) {
				continue;
			}
			WorkoutGenerationKey key = WorkoutGenerationKey.of(
					plan.id(), placement.day().id(), placement.date(), placement.cycle());
			Optional<WorkoutOccurrence> existing = workoutOccurrenceRepository.findByGenerationKey(key);
			if (existing.isPresent()) {
				if (existing.get().status() == WorkoutOccurrenceStatus.CANCELLED) {
					cancelledPlacementCount++;
				}
				else {
					existingCount++;
				}
				continue;
			}
			boolean slotTaken = workoutOccurrenceRepository
					.existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
							placement.day().id(), athleteId, placement.date(), WorkoutOccurrenceStatus.CANCELLED);
			if (slotTaken) {
				existingCount++;
				continue;
			}
			created.add(materialise(plan, athleteId, placement, key, exercisesByDay));
		}

		LocalDate ceiling = TrainingScheduleSupport.generationCeiling(plan, days);
		plan.advanceGeneratedThrough(to, ceiling, clock);
		TrainingPlan savedPlan = trainingPlanRepository.save(plan);

		return new WorkoutOccurrenceGenerationResult(
				from,
				to,
				created.size(),
				existingCount,
				cancelledPlacementCount,
				outOfScheduleCount,
				savedPlan.scheduleGeneratedThrough(),
				List.copyOf(created));
	}

	private WorkoutOccurrenceResult materialise(
			TrainingPlan plan,
			AthleteId athleteId,
			Placement placement,
			WorkoutGenerationKey key,
			Map<WorkoutDayId, List<WorkoutExercise>> exercisesByDay) {
		WorkoutDay day = placement.day();
		List<WorkoutExercise> exercises = exercisesByDay.computeIfAbsent(
				day.id(), id -> workoutExerciseRepository.findAllByWorkoutDayIdAndAthleteId(id, athleteId));
		if (exercises.isEmpty()) {
			throw new WorkoutOccurrenceRequiresExercisesException();
		}

		WorkoutOccurrenceId occurrenceId = WorkoutOccurrenceId.generate();
		WorkoutOccurrence occurrence = WorkoutOccurrence.createGenerated(
				occurrenceId,
				plan.id(),
				day.id(),
				athleteId,
				placement.date(),
				day.plannedStartTime(),
				key,
				clock);

		List<WorkoutExerciseExecution> executions = new ArrayList<>(exercises.size());
		for (WorkoutExercise exercise : exercises) {
			executions.add(WorkoutExerciseExecution.fromPrescription(exercise, occurrenceId, clock));
		}

		WorkoutOccurrence saved = workoutOccurrenceRepository.save(occurrence);
		List<WorkoutExerciseExecution> savedExecutions = workoutExerciseExecutionRepository.saveAll(executions);
		List<WorkoutExerciseSet> sets = new ArrayList<>();
		for (WorkoutExerciseExecution execution : savedExecutions) {
			sets.addAll(WorkoutExerciseSetSupport.createInitialSets(execution, clock));
		}
		workoutExerciseSetRepository.saveAll(sets);
		return WorkoutOccurrenceSupport.toResult(saved);
	}

	private static List<Placement> enumeratePlacements(
			TrainingPlan plan,
			List<WorkoutDay> days,
			LocalDate from,
			LocalDate to) {
		LocalDate scheduleStart = plan.scheduleStartDate();
		LocalDate scheduleEnd = plan.scheduleEndDate();
		int maxWeek = TrainingScheduleSupport.maxPlanWeekNumber(days);
		boolean repeating = plan.recurrenceMode() == TrainingPlanRecurrenceMode.REPEATING;

		int firstCycle = 1;
		int lastCycle = 1;
		if (repeating) {
			long cycleSpanDays = maxWeek * 7L;
			long fromOffset = ChronoUnit.DAYS.between(scheduleStart, from);
			long toOffset = ChronoUnit.DAYS.between(scheduleStart, to);
			if (toOffset < 0) {
				return List.of();
			}
			firstCycle = (int) Math.max(1, Math.floorDiv(fromOffset, cycleSpanDays) + 1);
			lastCycle = (int) Math.max(1, Math.floorDiv(toOffset, cycleSpanDays) + 1);
			// A placement can land one cycle later than its own week window when the range boundary
			// falls mid-cycle, so widen by one on each side and let the range filter decide.
			firstCycle = Math.max(1, firstCycle - 1);
			lastCycle = lastCycle + 1;
		}

		List<Placement> placements = new ArrayList<>();
		for (int cycle = firstCycle; cycle <= lastCycle; cycle++) {
			for (WorkoutDay day : days) {
				int week = (cycle - 1) * maxWeek + day.planWeekNumber();
				LocalDate date = TrainingScheduleDateCalculator.placementDate(
						scheduleStart, week, day.scheduledDayOfWeek());
				if (date.isBefore(from) || date.isAfter(to)) {
					continue;
				}
				boolean outOfSchedule = scheduleEnd != null && date.isAfter(scheduleEnd);
				placements.add(new Placement(day, date, cycle, outOfSchedule));
			}
		}
		placements.sort(Comparator
				.comparing(Placement::date)
				.thenComparing(placement -> placement.day().displayOrder()));
		return placements;
	}

	private record Placement(WorkoutDay day, LocalDate date, int cycle, boolean outOfSchedule) {
	}

}
