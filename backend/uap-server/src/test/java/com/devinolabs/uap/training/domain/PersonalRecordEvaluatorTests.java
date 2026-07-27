package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PersonalRecordEvaluatorTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-03-02T08:00:00Z"), ZoneOffset.UTC);

	private static final Instant EARLIER = Instant.parse("2026-03-01T12:00:00Z");

	private static final Instant LATER = Instant.parse("2026-03-08T12:00:00Z");

	private static final AthleteId ATHLETE_ID = AthleteId.of(UUID.randomUUID());

	private static final ExercisePerformanceKey KEY = ExercisePerformanceKey.of(UUID.randomUUID());

	private static final PersonalRecordProvenance PROVENANCE = new PersonalRecordProvenance(
			"Back Squat",
			WorkoutExerciseExecutionId.generate(),
			WorkoutOccurrenceId.generate(),
			LocalDate.of(2026, 3, 1));

	@Test
	void establishesASlotThatHasNoRecordYet() {
		assertThat(PersonalRecordEvaluator.evaluate(candidate("100", EARLIER, setId()), null))
				.isEqualTo(PersonalRecordEvaluator.Outcome.ESTABLISHED);
	}

	@Test
	void improvesOnAStrictlyLowerRecord() {
		AthleteExercisePersonalRecord current = record(candidate("100", EARLIER, setId()));

		PersonalRecordEvaluator.Outcome outcome = PersonalRecordEvaluator.evaluate(
				candidate("120", LATER, setId()), current);

		assertThat(outcome).isEqualTo(PersonalRecordEvaluator.Outcome.IMPROVED);
		assertThat(outcome.appendsHistory()).isTrue();
		assertThat(outcome.writesProjection()).isTrue();
	}

	@Test
	void leavesAHigherRecordAlone() {
		AthleteExercisePersonalRecord current = record(candidate("120", EARLIER, setId()));

		PersonalRecordEvaluator.Outcome outcome = PersonalRecordEvaluator.evaluate(
				candidate("100", LATER, setId()), current);

		assertThat(outcome).isEqualTo(PersonalRecordEvaluator.Outcome.UNCHANGED);
		assertThat(outcome.writesProjection()).isFalse();
	}

	@Test
	void keepsTheEarlierAchievementOnAnExactTie() {
		AthleteExercisePersonalRecord current = record(candidate("120", EARLIER, setId()));

		assertThat(PersonalRecordEvaluator.evaluate(candidate("120", LATER, setId()), current))
				.isEqualTo(PersonalRecordEvaluator.Outcome.UNCHANGED);
	}

	@Test
	void treatsPoundAndKilogramLogsOfTheSameLoadAsATie() {
		BigDecimal fromPounds = UnitNormalizationService
				.normalizeWeight(new BigDecimal("225"), WeightUnit.POUND).kilograms();
		BigDecimal fromKilograms = UnitNormalizationService
				.normalizeWeight(new BigDecimal("102.0582825"), WeightUnit.KILOGRAM).kilograms();
		AthleteExercisePersonalRecord current = record(
				candidate(fromPounds.toPlainString(), EARLIER, setId()));

		assertThat(PersonalRecordEvaluator.evaluate(
				candidate(fromKilograms.toPlainString(), LATER, setId()), current))
				.isEqualTo(PersonalRecordEvaluator.Outcome.UNCHANGED);
	}

	@Test
	void fallsBackToTheLowerSetUuidWhenTiesShareATimestamp() {
		WorkoutExerciseSetId lower = WorkoutExerciseSetId.of(new UUID(0L, 1L));
		WorkoutExerciseSetId higher = WorkoutExerciseSetId.of(new UUID(0L, 2L));
		AthleteExercisePersonalRecord current = record(candidate("120", EARLIER, higher));

		assertThat(PersonalRecordEvaluator.evaluate(candidate("120", EARLIER, lower), current))
				.isEqualTo(PersonalRecordEvaluator.Outcome.REPROVENANCED);
		assertThat(PersonalRecordEvaluator.Outcome.REPROVENANCED.appendsHistory()).isFalse();
		assertThat(PersonalRecordEvaluator.Outcome.REPROVENANCED.writesProjection()).isTrue();
	}

	@Test
	void picksTheSameWinnerRegardlessOfInputOrder() {
		PersonalRecordCandidate best = candidate("120", EARLIER, setId());
		PersonalRecordCandidate tie = candidate("120", LATER, setId());
		PersonalRecordCandidate weaker = candidate("100", EARLIER, setId());

		assertThat(PersonalRecordEvaluator.best(List.of(weaker, tie, best))).isEqualTo(best);
		assertThat(PersonalRecordEvaluator.best(List.of(best, weaker, tie))).isEqualTo(best);
		assertThat(PersonalRecordEvaluator.best(List.of())).isNull();
	}

	private static WorkoutExerciseSetId setId() {
		return WorkoutExerciseSetId.generate();
	}

	private static PersonalRecordCandidate candidate(String kilograms, Instant achievedAt, WorkoutExerciseSetId setId) {
		return new PersonalRecordCandidate(
				PersonalRecordType.HEAVIEST_WEIGHT,
				null,
				PerformanceMeasurement.measured(
						new BigDecimal(kilograms),
						PersonalRecordMeasure.KILOGRAM,
						new BigDecimal(kilograms),
						WeightUnit.KILOGRAM.name()),
				5,
				new BigDecimal(kilograms),
				WeightUnit.KILOGRAM,
				setId,
				achievedAt);
	}

	private static AthleteExercisePersonalRecord record(PersonalRecordCandidate candidate) {
		return AthleteExercisePersonalRecord.fromCandidate(ATHLETE_ID, KEY, candidate, PROVENANCE, CLOCK);
	}

}
