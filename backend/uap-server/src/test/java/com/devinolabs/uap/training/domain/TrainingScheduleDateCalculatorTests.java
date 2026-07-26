package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class TrainingScheduleDateCalculatorTests {

	/** Wednesday. */
	private static final LocalDate START = LocalDate.of(2026, 8, 5);

	@Test
	void weekWindowsAreFixedSevenDayBlocksFromScheduleStart() {
		assertThat(TrainingScheduleDateCalculator.weekWindowStart(START, 1)).isEqualTo(LocalDate.of(2026, 8, 5));
		assertThat(TrainingScheduleDateCalculator.weekWindowEnd(START, 1)).isEqualTo(LocalDate.of(2026, 8, 11));
		assertThat(TrainingScheduleDateCalculator.weekWindowStart(START, 2)).isEqualTo(LocalDate.of(2026, 8, 12));
		assertThat(TrainingScheduleDateCalculator.weekWindowEnd(START, 3)).isEqualTo(LocalDate.of(2026, 8, 25));
	}

	@Test
	void mondayInWeekOneOfAWednesdayStartFallsAtTheEndOfTheWindow() {
		assertThat(TrainingScheduleDateCalculator.placementDate(START, 1, DayOfWeek.MONDAY))
				.isEqualTo(LocalDate.of(2026, 8, 10));
	}

	@Test
	void placementsAlwaysLandInsideTheirOwnWeekWindow() {
		for (int week = 1; week <= 4; week++) {
			for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
				LocalDate date = TrainingScheduleDateCalculator.placementDate(START, week, dayOfWeek);
				assertThat(date.getDayOfWeek()).isEqualTo(dayOfWeek);
				assertThat(date).isBetween(
						TrainingScheduleDateCalculator.weekWindowStart(START, week),
						TrainingScheduleDateCalculator.weekWindowEnd(START, week));
			}
		}
	}

	@Test
	void startDayOfWeekMapsToTheWindowStartItself() {
		assertThat(TrainingScheduleDateCalculator.placementDate(START, 1, DayOfWeek.WEDNESDAY)).isEqualTo(START);
		assertThat(TrainingScheduleDateCalculator.placementDate(START, 2, DayOfWeek.WEDNESDAY))
				.isEqualTo(LocalDate.of(2026, 8, 12));
	}

	@Test
	void rejectsNonPositiveWeekNumbers() {
		assertThatThrownBy(() -> TrainingScheduleDateCalculator.weekWindowStart(START, 0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TrainingScheduleDateCalculator.placementDate(START, -1, DayOfWeek.MONDAY))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
