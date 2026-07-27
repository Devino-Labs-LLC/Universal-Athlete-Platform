package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * A performance figure carried in both the canonical comparison unit and the unit the athlete
 * actually logged, so readers never have to guess which one they are looking at.
 *
 * @param estimated true when the value comes from a formula (for example Epley) rather than a
 *                  direct measurement
 */
public record PerformanceMeasurement(
		BigDecimal normalizedValue,
		PersonalRecordMeasure normalizedUnit,
		BigDecimal measuredValue,
		String measuredUnit,
		boolean estimated) {

	public PerformanceMeasurement {
		Objects.requireNonNull(normalizedValue, "normalizedValue must not be null");
		Objects.requireNonNull(normalizedUnit, "normalizedUnit must not be null");
		normalizedValue = UnitNormalizationService.toMeasurementScale(normalizedValue);
		if (measuredValue != null) {
			measuredValue = UnitNormalizationService.toMeasurementScale(measuredValue);
		}
		if ((measuredValue == null) != (measuredUnit == null)) {
			throw new InvalidPerformanceMeasurementException(
					"measuredValue and measuredUnit must both be provided or both omitted");
		}
	}

	public static PerformanceMeasurement measured(
			BigDecimal normalizedValue,
			PersonalRecordMeasure normalizedUnit,
			BigDecimal measuredValue,
			String measuredUnit) {
		return new PerformanceMeasurement(normalizedValue, normalizedUnit, measuredValue, measuredUnit, false);
	}

	public static PerformanceMeasurement estimated(
			BigDecimal normalizedValue,
			PersonalRecordMeasure normalizedUnit,
			BigDecimal measuredValue,
			String measuredUnit) {
		return new PerformanceMeasurement(normalizedValue, normalizedUnit, measuredValue, measuredUnit, true);
	}

}
