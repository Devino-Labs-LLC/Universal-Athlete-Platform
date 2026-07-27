package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Turns the completed sets of an exercise execution into performance metrics and personal record
 * candidates.
 *
 * <p>Only COMPLETED sets contribute: skipped, in-progress and not-started sets never produce a
 * metric or a record. Occurrence- and execution-level eligibility is enforced by the caller.
 */
public final class ExercisePerformanceMetricCalculator {

	public static final String REPETITION_UNIT = "REPETITION";

	public static final String SECOND_UNIT = "SECOND";

	private static final String REPETITION_SUFFIX = "_REPETITION";

	private ExercisePerformanceMetricCalculator() {
	}

	public static List<WorkoutExerciseSet> eligibleSets(List<WorkoutExerciseSet> sets) {
		Objects.requireNonNull(sets, "sets must not be null");
		return sets.stream()
				.filter(set -> set.status() == WorkoutExerciseSetStatus.COMPLETED)
				.filter(set -> set.completedAt() != null)
				.sorted(Comparator
						.comparing(WorkoutExerciseSet::completedAt)
						.thenComparing(set -> set.id().value()))
				.toList();
	}

	public static ExercisePerformanceMetrics calculate(List<WorkoutExerciseSet> sets) {
		List<WorkoutExerciseSet> eligible = eligibleSets(sets);
		if (eligible.isEmpty()) {
			return ExercisePerformanceMetrics.empty();
		}

		Integer totalRepetitions = null;
		Integer mostRepetitionsInSet = null;
		Integer longestSetDurationSeconds = null;
		Integer totalDurationSeconds = null;
		PerformanceMeasurement heaviestWeight = null;
		PerformanceMeasurement bestEstimatedOneRepMax = null;
		PerformanceMeasurement bestSetVolume = null;
		PerformanceMeasurement longestSetDistance = null;
		BigDecimal totalVolumeKilograms = null;
		BigDecimal totalDistanceMeters = null;
		WeightUnit sharedWeightUnit = null;
		DistanceUnit sharedDistanceUnit = null;
		boolean weightUnitConsistent = true;
		boolean distanceUnitConsistent = true;
		BigDecimal rpeTotal = BigDecimal.ZERO;
		int rpeCount = 0;

		for (WorkoutExerciseSet set : eligible) {
			Integer reps = positive(set.actualReps());
			if (reps != null) {
				totalRepetitions = totalRepetitions == null ? reps : totalRepetitions + reps;
				mostRepetitionsInSet = mostRepetitionsInSet == null
						? reps
						: Math.max(mostRepetitionsInSet, reps);
			}

			Integer duration = positive(set.actualDurationSeconds());
			if (duration != null) {
				totalDurationSeconds = totalDurationSeconds == null ? duration : totalDurationSeconds + duration;
				longestSetDurationSeconds = longestSetDurationSeconds == null
						? duration
						: Math.max(longestSetDurationSeconds, duration);
			}

			if (set.actualRpe() != null) {
				rpeTotal = rpeTotal.add(set.actualRpe());
				rpeCount++;
			}

			NormalizedWeight weight = loadedWeight(set);
			if (weight != null) {
				if (sharedWeightUnit == null) {
					sharedWeightUnit = set.actualWeightUnit();
				}
				else if (sharedWeightUnit != set.actualWeightUnit()) {
					weightUnitConsistent = false;
				}
				heaviestWeight = better(heaviestWeight, PerformanceMeasurement.measured(
						weight.kilograms(),
						PersonalRecordMeasure.KILOGRAM,
						set.actualWeight(),
						set.actualWeightUnit().name()));

				NormalizedWeight oneRepMax = EstimatedOneRepMaxCalculator.estimate(
						set.actualWeight(), set.actualWeightUnit(), reps);
				if (oneRepMax != null) {
					bestEstimatedOneRepMax = better(bestEstimatedOneRepMax, PerformanceMeasurement.estimated(
							oneRepMax.kilograms(),
							PersonalRecordMeasure.KILOGRAM,
							UnitNormalizationService.denormalizeWeight(
									oneRepMax.kilograms(), set.actualWeightUnit()),
							set.actualWeightUnit().name()));
				}

				SetVolume volume = UnitNormalizationService.volumeOf(
						set.actualWeight(), set.actualWeightUnit(), reps);
				if (volume != null) {
					totalVolumeKilograms = totalVolumeKilograms == null
							? volume.kilogramRepetitions()
							: totalVolumeKilograms.add(volume.kilogramRepetitions());
					bestSetVolume = better(bestSetVolume, PerformanceMeasurement.measured(
							volume.kilogramRepetitions(),
							PersonalRecordMeasure.KILOGRAM_REPETITION,
							set.actualWeight().multiply(BigDecimal.valueOf(reps)),
							volumeUnit(set.actualWeightUnit())));
				}
			}

			NormalizedDistance distance = loggedDistance(set);
			if (distance != null) {
				if (sharedDistanceUnit == null) {
					sharedDistanceUnit = set.actualDistanceUnit();
				}
				else if (sharedDistanceUnit != set.actualDistanceUnit()) {
					distanceUnitConsistent = false;
				}
				totalDistanceMeters = totalDistanceMeters == null
						? distance.meters()
						: totalDistanceMeters.add(distance.meters());
				longestSetDistance = better(longestSetDistance, PerformanceMeasurement.measured(
						distance.meters(),
						PersonalRecordMeasure.METER,
						set.actualDistance(),
						set.actualDistanceUnit().name()));
			}
		}

		return new ExercisePerformanceMetrics(
				eligible.size(),
				totalRepetitions,
				mostRepetitionsInSet,
				heaviestWeight,
				bestEstimatedOneRepMax,
				bestSetVolume,
				totalVolume(totalVolumeKilograms, weightUnitConsistent ? sharedWeightUnit : null),
				longestSetDurationSeconds,
				totalDurationSeconds,
				longestSetDistance,
				totalDistance(totalDistanceMeters, distanceUnitConsistent ? sharedDistanceUnit : null),
				rpeCount == 0
						? null
						: UnitNormalizationService.toRpeScale(
								rpeTotal.divide(BigDecimal.valueOf(rpeCount), java.math.MathContext.DECIMAL128)));
	}

	public static List<PersonalRecordCandidate> candidates(List<WorkoutExerciseSet> sets) {
		List<PersonalRecordCandidate> candidates = new ArrayList<>();
		for (WorkoutExerciseSet set : eligibleSets(sets)) {
			Integer reps = positive(set.actualReps());
			NormalizedWeight weight = loadedWeight(set);

			if (weight != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.HEAVIEST_WEIGHT,
						null,
						PerformanceMeasurement.measured(
								weight.kilograms(),
								PersonalRecordMeasure.KILOGRAM,
								set.actualWeight(),
								set.actualWeightUnit().name()),
						reps));
			}

			if (reps != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.MOST_REPETITIONS,
						null,
						PerformanceMeasurement.measured(
								BigDecimal.valueOf(reps),
								PersonalRecordMeasure.REPETITION,
								BigDecimal.valueOf(reps),
								REPETITION_UNIT),
						reps));
			}

			if (weight != null && reps != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.MOST_REPETITIONS_AT_WEIGHT,
						weightQualifier(weight),
						PerformanceMeasurement.measured(
								BigDecimal.valueOf(reps),
								PersonalRecordMeasure.REPETITION,
								BigDecimal.valueOf(reps),
								REPETITION_UNIT),
						reps));
			}

			NormalizedWeight oneRepMax = weight == null
					? null
					: EstimatedOneRepMaxCalculator.estimate(set.actualWeight(), set.actualWeightUnit(), reps);
			if (oneRepMax != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.HIGHEST_ESTIMATED_ONE_REP_MAX,
						null,
						PerformanceMeasurement.estimated(
								oneRepMax.kilograms(),
								PersonalRecordMeasure.KILOGRAM,
								UnitNormalizationService.denormalizeWeight(
										oneRepMax.kilograms(), set.actualWeightUnit()),
								set.actualWeightUnit().name()),
						reps));
			}

			SetVolume volume = weight == null
					? null
					: UnitNormalizationService.volumeOf(set.actualWeight(), set.actualWeightUnit(), reps);
			if (volume != null && volume.isPositive()) {
				candidates.add(candidate(
						set,
						PersonalRecordType.HIGHEST_SET_VOLUME,
						null,
						PerformanceMeasurement.measured(
								volume.kilogramRepetitions(),
								PersonalRecordMeasure.KILOGRAM_REPETITION,
								set.actualWeight().multiply(BigDecimal.valueOf(reps)),
								volumeUnit(set.actualWeightUnit())),
						reps));
			}

			Integer duration = positive(set.actualDurationSeconds());
			if (duration != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.LONGEST_DURATION,
						null,
						PerformanceMeasurement.measured(
								BigDecimal.valueOf(duration),
								PersonalRecordMeasure.SECOND,
								BigDecimal.valueOf(duration),
								SECOND_UNIT),
						reps));
			}

			NormalizedDistance distance = loggedDistance(set);
			if (distance != null) {
				candidates.add(candidate(
						set,
						PersonalRecordType.LONGEST_DISTANCE,
						null,
						PerformanceMeasurement.measured(
								distance.meters(),
								PersonalRecordMeasure.METER,
								set.actualDistance(),
								set.actualDistanceUnit().name()),
						reps));
			}
		}
		return List.copyOf(candidates);
	}

	/**
	 * The normalized-weight bucket a {@code MOST_REPETITIONS_AT_WEIGHT} record is kept under. Using
	 * the canonical kilogram value at the persisted scale means 225 lb and 102.0583 kg share one
	 * bucket.
	 */
	public static String weightQualifier(NormalizedWeight weight) {
		Objects.requireNonNull(weight, "weight must not be null");
		return weight.kilograms().toPlainString();
	}

	private static PersonalRecordCandidate candidate(
			WorkoutExerciseSet set,
			PersonalRecordType recordType,
			String qualifier,
			PerformanceMeasurement measurement,
			Integer repetitions) {
		return new PersonalRecordCandidate(
				recordType,
				qualifier,
				measurement,
				repetitions,
				set.actualWeight(),
				set.actualWeightUnit(),
				set.id(),
				set.completedAt());
	}

	private static PerformanceMeasurement totalVolume(BigDecimal kilogramRepetitions, WeightUnit sharedUnit) {
		if (kilogramRepetitions == null) {
			return null;
		}
		BigDecimal normalized = UnitNormalizationService.toMeasurementScale(kilogramRepetitions);
		if (sharedUnit == null) {
			return new PerformanceMeasurement(
					normalized, PersonalRecordMeasure.KILOGRAM_REPETITION, null, null, false);
		}
		return PerformanceMeasurement.measured(
				normalized,
				PersonalRecordMeasure.KILOGRAM_REPETITION,
				UnitNormalizationService.denormalizeWeight(normalized, sharedUnit),
				volumeUnit(sharedUnit));
	}

	private static PerformanceMeasurement totalDistance(BigDecimal meters, DistanceUnit sharedUnit) {
		if (meters == null) {
			return null;
		}
		BigDecimal normalized = UnitNormalizationService.toMeasurementScale(meters);
		if (sharedUnit == null) {
			return new PerformanceMeasurement(normalized, PersonalRecordMeasure.METER, null, null, false);
		}
		return PerformanceMeasurement.measured(
				normalized,
				PersonalRecordMeasure.METER,
				UnitNormalizationService.denormalizeDistance(normalized, sharedUnit),
				sharedUnit.name());
	}

	private static PerformanceMeasurement better(PerformanceMeasurement current, PerformanceMeasurement candidate) {
		if (current == null) {
			return candidate;
		}
		return candidate.normalizedValue().compareTo(current.normalizedValue()) > 0 ? candidate : current;
	}

	private static NormalizedWeight loadedWeight(WorkoutExerciseSet set) {
		if (set.actualWeight() == null || set.actualWeightUnit() == null) {
			return null;
		}
		NormalizedWeight weight = UnitNormalizationService.normalizeWeight(
				set.actualWeight(), set.actualWeightUnit());
		return weight.isPositive() ? weight : null;
	}

	private static NormalizedDistance loggedDistance(WorkoutExerciseSet set) {
		if (set.actualDistance() == null || set.actualDistanceUnit() == null) {
			return null;
		}
		NormalizedDistance distance = UnitNormalizationService.normalizeDistance(
				set.actualDistance(), set.actualDistanceUnit());
		return distance.isPositive() ? distance : null;
	}

	private static String volumeUnit(WeightUnit unit) {
		return unit.name() + REPETITION_SUFFIX;
	}

	private static Integer positive(Integer value) {
		return value != null && value > 0 ? value : null;
	}

}
