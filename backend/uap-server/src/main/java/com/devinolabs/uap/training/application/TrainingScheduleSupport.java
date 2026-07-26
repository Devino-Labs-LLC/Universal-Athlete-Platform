package com.devinolabs.uap.training.application;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingScheduleDateCalculator;
import com.devinolabs.uap.training.domain.WorkoutDay;

final class TrainingScheduleSupport {

	static final int MAX_GENERATION_RANGE_DAYS = 90;
	static final int MAX_CALENDAR_RANGE_DAYS = 366;

	private TrainingScheduleSupport() {
	}

	static ZoneId requireZone(String timezone) {
		if (timezone == null || timezone.isBlank()) {
			throw new InvalidTimezoneException("timezone must not be blank");
		}
		try {
			return ZoneId.of(timezone.trim());
		}
		catch (DateTimeException ex) {
			throw new InvalidTimezoneException("timezone is not a valid IANA zone id: " + timezone);
		}
	}

	static int maxPlanWeekNumber(List<WorkoutDay> days) {
		return days.stream()
				.map(WorkoutDay::planWeekNumber)
				.filter(week -> week != null)
				.mapToInt(Integer::intValue)
				.max()
				.orElseThrow(() -> new TrainingPlanScheduleRequiresWorkoutDaysException(
						"At least one workout day must declare a planWeekNumber"));
	}

	/** Last date a FINITE plan can ever place a workout on. */
	static LocalDate finiteLastPlacementDate(LocalDate scheduleStartDate, List<WorkoutDay> days) {
		return days.stream()
				.filter(WorkoutDay::hasSchedulablePlacement)
				.map(day -> TrainingScheduleDateCalculator.placementDate(
						scheduleStartDate, day.planWeekNumber(), day.scheduledDayOfWeek()))
				.max(LocalDate::compareTo)
				.orElseThrow(() -> new TrainingPlanScheduleRequiresWorkoutDaysException(
						"At least one workout day must declare a placement"));
	}

	/** For FINITE plans, the generation watermark can never move past the last placement. */
	static LocalDate generationCeiling(TrainingPlan plan, List<WorkoutDay> days) {
		if (plan.recurrenceMode() == TrainingPlanRecurrenceMode.FINITE) {
			return finiteLastPlacementDate(plan.scheduleStartDate(), days);
		}
		return null;
	}

	static void requireScheduleConfigured(TrainingPlan plan) {
		if (plan.scheduleStartDate() == null || plan.scheduleTimezone() == null
				|| plan.recurrenceMode() == null) {
			throw new TrainingPlanScheduleNotConfiguredException(
					"Training plan schedule has not been configured");
		}
	}

	static void requireScheduleActive(TrainingPlan plan) {
		if (!plan.isScheduleActive()) {
			throw new InvalidTrainingPlanScheduleStatusException(
					"Schedule must be ACTIVE but was " + plan.scheduleStatus());
		}
	}

	static RuntimeException translateScheduleState(IllegalStateException ex) {
		return new InvalidTrainingPlanScheduleStatusException(
				ex.getMessage() == null ? "Invalid schedule transition" : ex.getMessage());
	}

}
