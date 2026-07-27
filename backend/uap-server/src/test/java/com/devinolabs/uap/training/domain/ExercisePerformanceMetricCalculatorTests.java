package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExercisePerformanceMetricCalculatorTests {

	private static final Instant BASE = Instant.parse("2026-03-01T12:00:00Z");

	@Test
	void aggregatesAHeavyBarbellSession() {
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(backSquatSession());

		assertThat(metrics.completedSetCount()).isEqualTo(5);
		// 5+5+5+4+6 — Set 4 is 225×4 per the Phase 7H acceptance scenario.
		assertThat(metrics.totalRepetitions()).isEqualTo(25);
		assertThat(metrics.mostRepetitionsInSet()).isEqualTo(6);
		assertThat(metrics.heaviestWeight().normalizedValue()).isEqualByComparingTo("102.0583");
		assertThat(metrics.heaviestWeight().measuredValue()).isEqualByComparingTo("225");
		assertThat(metrics.heaviestWeight().measuredUnit()).isEqualTo("POUND");
		assertThat(metrics.heaviestWeight().estimated()).isFalse();
		assertThat(metrics.bestEstimatedOneRepMax().normalizedValue()).isEqualByComparingTo("119.0680");
		assertThat(metrics.bestEstimatedOneRepMax().measuredValue()).isEqualByComparingTo("262.5000");
		assertThat(metrics.bestEstimatedOneRepMax().estimated()).isTrue();
		// 205 lb × 6 (1230 lb-reps) beats 225 lb × 5 (1125 lb-reps) on set volume.
		assertThat(metrics.bestSetVolume().normalizedValue()).isEqualByComparingTo("557.9184");
		assertThat(metrics.bestSetVolume().measuredValue()).isEqualByComparingTo("1230");
		assertThat(metrics.bestSetVolume().measuredUnit()).isEqualTo("POUND_REPETITION");
		// (102.0583×5)×3 + (102.0583×4) + (92.9864×6)
		assertThat(metrics.totalVolume().normalizedValue()).isEqualByComparingTo("2497.0261");
		assertThat(metrics.totalDurationSeconds()).isNull();
		assertThat(metrics.totalDistance()).isNull();
	}

	@Test
	void ignoresEverySetThatIsNotCompleted() {
		WorkoutExerciseSet completed = PerformanceTestSets.completedLift(
				WorkoutExerciseSetId.generate(), 1, 5, "100", WeightUnit.KILOGRAM, BASE);
		List<WorkoutExerciseSet> sets = List.of(
				completed,
				PerformanceTestSets.ineligible(WorkoutExerciseSetId.generate(), 2, 20, "200",
						WeightUnit.KILOGRAM, WorkoutExerciseSetStatus.SKIPPED),
				PerformanceTestSets.ineligible(WorkoutExerciseSetId.generate(), 3, 30, "300",
						WeightUnit.KILOGRAM, WorkoutExerciseSetStatus.IN_PROGRESS),
				PerformanceTestSets.ineligible(WorkoutExerciseSetId.generate(), 4, 40, "400",
						WeightUnit.KILOGRAM, WorkoutExerciseSetStatus.NOT_STARTED));

		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(sets);

		assertThat(ExercisePerformanceMetricCalculator.eligibleSets(sets)).containsExactly(completed);
		assertThat(metrics.completedSetCount()).isEqualTo(1);
		assertThat(metrics.totalRepetitions()).isEqualTo(5);
		assertThat(metrics.heaviestWeight().normalizedValue()).isEqualByComparingTo("100.0000");
	}

	@Test
	void producesEmptyMetricsWhenNothingWasCompleted() {
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(List.of(
				PerformanceTestSets.ineligible(WorkoutExerciseSetId.generate(), 1, 5, "100",
						WeightUnit.KILOGRAM, WorkoutExerciseSetStatus.SKIPPED)));

		assertThat(metrics.completedSetCount()).isZero();
		assertThat(metrics.totalRepetitions()).isNull();
		assertThat(metrics.heaviestWeight()).isNull();
	}

	@Test
	void leavesBodyweightWorkWithoutWeightOrVolume() {
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(List.of(
				PerformanceTestSets.completedBodyweight(WorkoutExerciseSetId.generate(), 1, 12, BASE),
				PerformanceTestSets.completedBodyweight(
						WorkoutExerciseSetId.generate(), 2, 10, BASE.plusSeconds(60))));

		assertThat(metrics.totalRepetitions()).isEqualTo(22);
		assertThat(metrics.mostRepetitionsInSet()).isEqualTo(12);
		assertThat(metrics.heaviestWeight()).isNull();
		assertThat(metrics.bestSetVolume()).isNull();
		assertThat(metrics.totalVolume()).isNull();
		assertThat(metrics.bestEstimatedOneRepMax()).isNull();
	}

	@Test
	void aggregatesHoldsAndDistances() {
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(List.of(
				PerformanceTestSets.completedHold(WorkoutExerciseSetId.generate(), 1, 45, BASE),
				PerformanceTestSets.completedHold(WorkoutExerciseSetId.generate(), 2, 75, BASE.plusSeconds(60)),
				PerformanceTestSets.completedDistance(WorkoutExerciseSetId.generate(), 3, "1",
						DistanceUnit.KILOMETER, BASE.plusSeconds(120)),
				PerformanceTestSets.completedDistance(WorkoutExerciseSetId.generate(), 4, "400",
						DistanceUnit.METER, BASE.plusSeconds(180))));

		assertThat(metrics.longestSetDurationSeconds()).isEqualTo(75);
		assertThat(metrics.totalDurationSeconds()).isEqualTo(120);
		assertThat(metrics.longestSetDistance().normalizedValue()).isEqualByComparingTo("1000.0000");
		assertThat(metrics.totalDistance().normalizedValue()).isEqualByComparingTo("1400.0000");
		assertThat(metrics.totalDistance().measuredValue()).isNull();
	}

	@Test
	void averagesRpeAcrossTheSetsThatReportedIt() {
		ExercisePerformanceMetrics metrics = ExercisePerformanceMetricCalculator.calculate(List.of(
				PerformanceTestSets.completedWithRpe(
						WorkoutExerciseSetId.generate(), 1, 5, "100", WeightUnit.KILOGRAM, "8", BASE),
				PerformanceTestSets.completedWithRpe(WorkoutExerciseSetId.generate(), 2, 5, "100",
						WeightUnit.KILOGRAM, "9", BASE.plusSeconds(60)),
				PerformanceTestSets.completedLift(WorkoutExerciseSetId.generate(), 3, 5, "100",
						WeightUnit.KILOGRAM, BASE.plusSeconds(120))));

		assertThat(metrics.averageRpe()).isEqualByComparingTo("8.50");
	}

	@Test
	void derivesOneCandidatePerRecordDimension() {
		List<PersonalRecordCandidate> candidates = ExercisePerformanceMetricCalculator.candidates(List.of(
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 1, 5, "225", WeightUnit.POUND, BASE)));

		assertThat(candidates).extracting(PersonalRecordCandidate::recordType).containsExactlyInAnyOrder(
				PersonalRecordType.HEAVIEST_WEIGHT,
				PersonalRecordType.MOST_REPETITIONS,
				PersonalRecordType.MOST_REPETITIONS_AT_WEIGHT,
				PersonalRecordType.HIGHEST_ESTIMATED_ONE_REP_MAX,
				PersonalRecordType.HIGHEST_SET_VOLUME);
	}

	@Test
	void bucketsRepetitionsAtWeightByTheNormalizedKilogramValue() {
		List<PersonalRecordCandidate> pounds = ExercisePerformanceMetricCalculator.candidates(List.of(
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 1, 5, "225", WeightUnit.POUND, BASE)));
		List<PersonalRecordCandidate> kilograms = ExercisePerformanceMetricCalculator.candidates(List.of(
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 1, 8, "102.0582825", WeightUnit.KILOGRAM, BASE)));

		assertThat(qualifierOf(pounds)).isEqualTo("102.0583").isEqualTo(qualifierOf(kilograms));
	}

	@Test
	void producesNoVolumeOrOneRepMaxCandidatesForBodyweightWork() {
		List<PersonalRecordCandidate> candidates = ExercisePerformanceMetricCalculator.candidates(List.of(
				PerformanceTestSets.completedBodyweight(WorkoutExerciseSetId.generate(), 1, 20, BASE)));

		assertThat(candidates).extracting(PersonalRecordCandidate::recordType)
				.containsExactly(PersonalRecordType.MOST_REPETITIONS);
	}

	static List<WorkoutExerciseSet> backSquatSession() {
		return List.of(
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 1, 5, "225", WeightUnit.POUND, BASE),
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 2, 5, "225", WeightUnit.POUND, BASE.plusSeconds(180)),
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 3, 5, "225", WeightUnit.POUND, BASE.plusSeconds(360)),
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 4, 4, "225", WeightUnit.POUND, BASE.plusSeconds(540)),
				PerformanceTestSets.completedLift(
						WorkoutExerciseSetId.generate(), 5, 6, "205", WeightUnit.POUND, BASE.plusSeconds(720)));
	}

	private static String qualifierOf(List<PersonalRecordCandidate> candidates) {
		return candidates.stream()
				.filter(candidate -> candidate.recordType() == PersonalRecordType.MOST_REPETITIONS_AT_WEIGHT)
				.findFirst()
				.orElseThrow()
				.recordQualifier();
	}

}
