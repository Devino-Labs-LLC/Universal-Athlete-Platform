package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class DailyRecoveryCheckInDomainTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-31T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void fatigueRatingLabelsAndBounds() {
		assertThat(FatigueRating.of(1).label()).isEqualTo("VERY_LOW");
		assertThat(FatigueRating.of(5).label()).isEqualTo("VERY_HIGH");
		assertThatThrownBy(() -> FatigueRating.of(0)).isInstanceOf(InvalidFatigueRatingException.class);
		assertThatThrownBy(() -> FatigueRating.of(6)).isInstanceOf(InvalidFatigueRatingException.class);
	}

	@Test
	void muscleSorenessRatingLabelsAndBounds() {
		assertThat(MuscleSorenessRating.of(1).label()).isEqualTo("NONE_OR_MINIMAL");
		assertThat(MuscleSorenessRating.of(5).label()).isEqualTo("VERY_HIGH");
		assertThatThrownBy(() -> MuscleSorenessRating.of(0)).isInstanceOf(InvalidMuscleSorenessRatingException.class);
	}

	@Test
	void stressRatingLabelsAndBounds() {
		assertThat(StressRating.of(3).label()).isEqualTo("MODERATE");
		assertThatThrownBy(() -> StressRating.of(6)).isInstanceOf(InvalidStressRatingException.class);
	}

	@Test
	void moodRatingLabelsAndBounds() {
		assertThat(MoodRating.of(3).label()).isEqualTo("NEUTRAL");
		assertThat(MoodRating.of(5).label()).isEqualTo("VERY_GOOD");
		assertThatThrownBy(() -> MoodRating.of(0)).isInstanceOf(InvalidMoodRatingException.class);
	}

	@Test
	void motivationRatingLabelsAndBounds() {
		assertThat(TrainingMotivationRating.of(4).label()).isEqualTo("HIGH");
		assertThatThrownBy(() -> TrainingMotivationRating.of(6))
				.isInstanceOf(InvalidTrainingMotivationRatingException.class);
	}

	@Test
	void sleepQualityRatingLabelsAndBounds() {
		assertThat(SleepQualityRating.of(3).label()).isEqualTo("FAIR");
		assertThatThrownBy(() -> SleepQualityRating.of(0)).isInstanceOf(InvalidSleepQualityException.class);
	}

	@Test
	void sleepDurationValidation() {
		DailyRecoveryCheckIn.validateSleepDuration(null);
		DailyRecoveryCheckIn.validateSleepDuration(0);
		DailyRecoveryCheckIn.validateSleepDuration(1440);
		assertThatThrownBy(() -> DailyRecoveryCheckIn.validateSleepDuration(-1))
				.isInstanceOf(InvalidSleepDurationException.class);
		assertThatThrownBy(() -> DailyRecoveryCheckIn.validateSleepDuration(1441))
				.isInstanceOf(InvalidSleepDurationException.class);
	}

	@Test
	void datePolicyAllowsTodayAndPastThirtyDays() {
		LocalDate today = LocalDate.now(CLOCK);
		RecoveryCheckInDateValidator.validate(today, CLOCK);
		RecoveryCheckInDateValidator.validate(today.minusDays(30), CLOCK);
		assertThatThrownBy(() -> RecoveryCheckInDateValidator.validate(today.plusDays(1), CLOCK))
				.isInstanceOf(InvalidRecoveryCheckInDateException.class);
		assertThatThrownBy(() -> RecoveryCheckInDateValidator.validate(today.minusDays(31), CLOCK))
				.isInstanceOf(RecoveryCheckInDateOutOfRangeException.class);
	}

	@Test
	void completenessIsCompleteWhenRequiredRatingsPresent() {
		assertThat(RecoveryCheckInCompletenessCalculator.calculate(
				FatigueRating.of(3),
				MuscleSorenessRating.of(3),
				StressRating.of(2),
				MoodRating.of(4),
				TrainingMotivationRating.of(3)))
				.isEqualTo(RecoveryCheckInCompleteness.COMPLETE);
	}

	@Test
	void discomfortRejectsDuplicatesAndGeneralFullBodySide() {
		List<BodyAreaDiscomfortObservation.Input> inputs = List.of(
				new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, "Tight"),
				new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 3, "Dup"));
		assertThatThrownBy(() -> BodyAreaDiscomfortObservation.validateAndOrder(inputs))
				.isInstanceOf(DuplicateBodyAreaDiscomfortException.class);

		List<BodyAreaDiscomfortObservation.Input> invalidSide = List.of(
				new BodyAreaDiscomfortObservation.Input("GENERAL_FULL_BODY", "LEFT", 2, null));
		assertThatThrownBy(() -> BodyAreaDiscomfortObservation.validateAndOrder(invalidSide))
				.isInstanceOf(InvalidBodyAreaDiscomfortException.class);
	}

	@Test
	void updateNoOpDoesNotCreateRevision() {
		DailyRecoveryCheckIn checkIn = sampleCheckIn();
		assertThat(checkIn.update(checkIn.snapshot(), 1, CLOCK)).isEmpty();
	}

	@Test
	void updateCreatesRevisionWhenValuesChange() {
		DailyRecoveryCheckIn checkIn = sampleCheckIn();
		DailyRecoveryCheckIn.Snapshot updated = new DailyRecoveryCheckIn.Snapshot(
				checkIn.sleepDurationMinutes(),
				checkIn.sleepQuality(),
				FatigueRating.of(3),
				checkIn.muscleSoreness(),
				checkIn.stress(),
				checkIn.mood(),
				TrainingMotivationRating.of(4),
				checkIn.completeness(),
				List.of(),
				"Updated notes");
		assertThat(checkIn.update(updated, 1, CLOCK)).isPresent();
		assertThat(checkIn.fatigue().value()).isEqualTo(3);
		assertThat(checkIn.motivation().value()).isEqualTo(4);
		assertThat(checkIn.discomfortAreas()).isEmpty();
	}

	private static DailyRecoveryCheckIn sampleCheckIn() {
		return DailyRecoveryCheckIn.create(
				AthleteId.of(UUID.randomUUID()),
				LocalDate.of(2026, 7, 31),
				420,
				SleepQualityRating.of(3),
				FatigueRating.of(4),
				MuscleSorenessRating.of(3),
				StressRating.of(2),
				MoodRating.of(4),
				TrainingMotivationRating.of(3),
				BodyAreaDiscomfortObservation.validateAndOrder(List.of(
						new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, "Mild tightness"))),
				"Tired",
				RecoveryCheckInSource.ATHLETE_REPORTED,
				CLOCK);
	}

}
