package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Converts logged measurements into the canonical units personal records compare in: kilograms for
 * weight and metres for distance.
 *
 * <p>Conversions run at {@link MathContext#DECIMAL128} and are rounded HALF_UP to the persisted
 * scale, so two logs of the same physical load compare equal regardless of the unit they were
 * entered in.
 */
public final class UnitNormalizationService {

	/** Scale weights, distances, volumes and estimated one-rep maxima are persisted at. */
	public static final int MEASUREMENT_SCALE = 4;

	/** Scale averaged RPE is persisted at. */
	public static final int RPE_SCALE = 2;

	public static final BigDecimal KILOGRAMS_PER_POUND = new BigDecimal("0.45359237");

	private static final BigDecimal METERS_PER_KILOMETER = new BigDecimal("1000");

	private static final BigDecimal METERS_PER_MILE = new BigDecimal("1609.344");

	private static final MathContext CONTEXT = MathContext.DECIMAL128;

	private UnitNormalizationService() {
	}

	public static NormalizedWeight normalizeWeight(BigDecimal weight, WeightUnit unit) {
		requireMeasurement(weight, unit, "weight", "weightUnit");
		return NormalizedWeight.ofKilograms(switch (unit) {
			case KILOGRAM -> weight;
			case POUND -> weight.multiply(KILOGRAMS_PER_POUND, CONTEXT);
			default -> throw new UnsupportedWeightUnitException("Unsupported weight unit " + unit);
		});
	}

	/**
	 * Expresses a canonical kilogram value back in the unit the athlete logged in, so responses can
	 * echo the measured value alongside the normalized one.
	 */
	public static BigDecimal denormalizeWeight(BigDecimal kilograms, WeightUnit unit) {
		Objects.requireNonNull(kilograms, "kilograms must not be null");
		requireWeightUnit(unit);
		return toMeasurementScale(switch (unit) {
			case KILOGRAM -> kilograms;
			case POUND -> kilograms.divide(KILOGRAMS_PER_POUND, CONTEXT);
			default -> throw new UnsupportedWeightUnitException("Unsupported weight unit " + unit);
		});
	}

	public static NormalizedDistance normalizeDistance(BigDecimal distance, DistanceUnit unit) {
		requireMeasurement(distance, unit, "distance", "distanceUnit");
		return NormalizedDistance.ofMeters(switch (unit) {
			case METER -> distance;
			case KILOMETER -> distance.multiply(METERS_PER_KILOMETER, CONTEXT);
			case MILE -> distance.multiply(METERS_PER_MILE, CONTEXT);
			default -> throw new UnsupportedDistanceUnitException("Unsupported distance unit " + unit);
		});
	}

	public static BigDecimal denormalizeDistance(BigDecimal meters, DistanceUnit unit) {
		Objects.requireNonNull(meters, "meters must not be null");
		requireDistanceUnit(unit);
		return toMeasurementScale(switch (unit) {
			case METER -> meters;
			case KILOMETER -> meters.divide(METERS_PER_KILOMETER, CONTEXT);
			case MILE -> meters.divide(METERS_PER_MILE, CONTEXT);
			default -> throw new UnsupportedDistanceUnitException("Unsupported distance unit " + unit);
		});
	}

	/**
	 * External load moved by one set. Returns {@code null} when the set carries no external weight
	 * or no repetitions, which is how bodyweight-only work stays out of volume records.
	 */
	public static SetVolume volumeOf(BigDecimal weight, WeightUnit unit, Integer repetitions) {
		if (weight == null || unit == null || repetitions == null || repetitions <= 0) {
			return null;
		}
		NormalizedWeight normalized = normalizeWeight(weight, unit);
		if (!normalized.isPositive()) {
			return null;
		}
		return SetVolume.ofKilogramRepetitions(
				normalized.kilograms().multiply(BigDecimal.valueOf(repetitions), CONTEXT));
	}

	public static BigDecimal toMeasurementScale(BigDecimal value) {
		Objects.requireNonNull(value, "value must not be null");
		return value.setScale(MEASUREMENT_SCALE, RoundingMode.HALF_UP);
	}

	public static BigDecimal toRpeScale(BigDecimal value) {
		Objects.requireNonNull(value, "value must not be null");
		return value.setScale(RPE_SCALE, RoundingMode.HALF_UP);
	}

	private static void requireMeasurement(BigDecimal value, Enum<?> unit, String field, String unitField) {
		if (value == null) {
			throw new InvalidPerformanceMeasurementException(field + " must not be null");
		}
		if (unit == null) {
			throw new InvalidPerformanceMeasurementException(unitField + " must not be null");
		}
		if (value.signum() < 0) {
			throw new InvalidPerformanceMeasurementException(field + " must be >= 0");
		}
	}

	private static void requireWeightUnit(WeightUnit unit) {
		if (unit == null) {
			throw new UnsupportedWeightUnitException("weightUnit must not be null");
		}
	}

	private static void requireDistanceUnit(DistanceUnit unit) {
		if (unit == null) {
			throw new UnsupportedDistanceUnitException("distanceUnit must not be null");
		}
	}

}
