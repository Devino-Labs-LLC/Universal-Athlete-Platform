package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.PerformanceMeasurement;
import com.devinolabs.uap.training.domain.PersonalRecordMeasure;

/**
 * A performance figure in both the canonical comparison unit and the unit the athlete logged.
 *
 * @param estimated true when the value was calculated (Epley one-rep max) rather than measured
 */
record PerformanceMeasurementResponse(
		BigDecimal normalizedValue,
		PersonalRecordMeasure normalizedUnit,
		BigDecimal measuredValue,
		String measuredUnit,
		boolean estimated) {

	static PerformanceMeasurementResponse from(PerformanceMeasurement measurement) {
		if (measurement == null) {
			return null;
		}
		return new PerformanceMeasurementResponse(
				measurement.normalizedValue(),
				measurement.normalizedUnit(),
				measurement.measuredValue(),
				measurement.measuredUnit(),
				measurement.estimated());
	}

}
