package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Assembles calendar entries by decorating occurrences with plan/day labels and execution counts.
 */
@Component
class AthleteTrainingCalendarReader {

	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;

	AthleteTrainingCalendarReader(
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository) {
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
	}

	static void requireValidRange(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new InvalidTrainingCalendarRangeException("from and to are required");
		}
		if (to.isBefore(from)) {
			throw new InvalidTrainingCalendarRangeException("to must not be before from");
		}
		long days = ChronoUnit.DAYS.between(from, to) + 1;
		if (days > TrainingScheduleSupport.MAX_CALENDAR_RANGE_DAYS) {
			throw new InvalidTrainingCalendarRangeException(
					"Calendar range must not exceed " + TrainingScheduleSupport.MAX_CALENDAR_RANGE_DAYS + " days");
		}
	}

	List<AthleteCalendarEntryResult> read(
			AthleteId athleteId,
			LocalDate from,
			LocalDate to,
			WorkoutOccurrenceStatus status,
			TrainingPlanId trainingPlanId) {
		requireValidRange(from, to);
		if (trainingPlanId != null) {
			WorkoutDaySupport.requirePlan(trainingPlanRepository, athleteId, trainingPlanId);
		}

		List<WorkoutOccurrence> occurrences = workoutOccurrenceRepository
				.findCalendarRange(athleteId, from, to, status, trainingPlanId);
		if (occurrences.isEmpty()) {
			return List.of();
		}

		Map<TrainingPlanId, String> planNames = new HashMap<>();
		for (TrainingPlan plan : trainingPlanRepository.findAllByAthleteId(athleteId)) {
			planNames.put(plan.id(), plan.name());
		}
		Map<WorkoutDayId, String> dayTitles = new HashMap<>();
		List<WorkoutDayId> dayIds = occurrences.stream().map(WorkoutOccurrence::workoutDayId).distinct().toList();
		for (WorkoutDay day : workoutDayRepository.findAllByIdInAndAthleteId(dayIds, athleteId)) {
			dayTitles.put(day.id(), day.title());
		}

		List<WorkoutOccurrenceId> occurrenceIds = occurrences.stream().map(WorkoutOccurrence::id).toList();
		Map<WorkoutOccurrenceId, Map<WorkoutExerciseExecutionStatus, Long>> counts = new HashMap<>();
		for (WorkoutExerciseExecutionStatusCount row
				: workoutExerciseExecutionRepository.countByStatusForOccurrences(occurrenceIds, athleteId)) {
			counts.computeIfAbsent(row.occurrenceId(), id -> new EnumMap<>(WorkoutExerciseExecutionStatus.class))
					.put(row.status(), row.count());
		}

		List<AthleteCalendarEntryResult> entries = new ArrayList<>(occurrences.size());
		for (WorkoutOccurrence occurrence : occurrences) {
			Map<WorkoutExerciseExecutionStatus, Long> byStatus = counts.getOrDefault(occurrence.id(), Map.of());
			int notStarted = count(byStatus, WorkoutExerciseExecutionStatus.NOT_STARTED);
			int inProgress = count(byStatus, WorkoutExerciseExecutionStatus.IN_PROGRESS);
			int completed = count(byStatus, WorkoutExerciseExecutionStatus.COMPLETED);
			int skipped = count(byStatus, WorkoutExerciseExecutionStatus.SKIPPED);
			entries.add(new AthleteCalendarEntryResult(
					occurrence.id(),
					occurrence.trainingPlanId(),
					planNames.get(occurrence.trainingPlanId()),
					occurrence.workoutDayId(),
					dayTitles.get(occurrence.workoutDayId()),
					occurrence.scheduledDate(),
					occurrence.plannedStartTime(),
					occurrence.status(),
					occurrence.origin(),
					occurrence.manuallyRescheduled(),
					occurrence.originalScheduledDate(),
					occurrence.startedAt(),
					occurrence.completedAt(),
					occurrence.athleteNotes(),
					notStarted + inProgress + completed + skipped,
					notStarted,
					inProgress,
					completed,
					skipped));
		}
		return List.copyOf(entries);
	}

	private static int count(
			Map<WorkoutExerciseExecutionStatus, Long> byStatus,
			WorkoutExerciseExecutionStatus status) {
		return byStatus.getOrDefault(status, 0L).intValue();
	}

}
