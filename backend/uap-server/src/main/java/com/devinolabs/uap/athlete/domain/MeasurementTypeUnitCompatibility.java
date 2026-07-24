package com.devinolabs.uap.athlete.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Explicit measurement type/unit compatibility matrix. Compatibility is never inferred from names.
 */
public final class MeasurementTypeUnitCompatibility {

	private static final Map<MeasurementType, Set<MeasurementUnit>> ALLOWED = build();

	private MeasurementTypeUnitCompatibility() {
	}

	public static void requireCompatible(MeasurementType type, MeasurementUnit unit) {
		Objects.requireNonNull(type, "measurementType must not be null");
		Objects.requireNonNull(unit, "unit must not be null");
		if (type == MeasurementType.OTHER) {
			return;
		}
		Set<MeasurementUnit> allowed = ALLOWED.get(type);
		if (allowed == null || !allowed.contains(unit)) {
			throw new IllegalArgumentException(
					"measurementUnit " + unit + " is incompatible with measurementType " + type);
		}
	}

	public static boolean isCompatible(MeasurementType type, MeasurementUnit unit) {
		try {
			requireCompatible(type, unit);
			return true;
		}
		catch (IllegalArgumentException ex) {
			return false;
		}
	}

	private static Map<MeasurementType, Set<MeasurementUnit>> build() {
		Map<MeasurementType, Set<MeasurementUnit>> map = new EnumMap<>(MeasurementType.class);
		Set<MeasurementUnit> mass = EnumSet.of(MeasurementUnit.KILOGRAM, MeasurementUnit.POUND);
		Set<MeasurementUnit> circumference = EnumSet.of(
				MeasurementUnit.CENTIMETER, MeasurementUnit.INCH, MeasurementUnit.MILLIMETER);
		Set<MeasurementUnit> heartRate = EnumSet.of(MeasurementUnit.BEATS_PER_MINUTE);
		Set<MeasurementUnit> bloodPressure = EnumSet.of(MeasurementUnit.MILLIMETERS_OF_MERCURY);
		Set<MeasurementUnit> jump = EnumSet.of(MeasurementUnit.CENTIMETER, MeasurementUnit.INCH, MeasurementUnit.METER);
		Set<MeasurementUnit> time = EnumSet.of(
				MeasurementUnit.MILLISECOND, MeasurementUnit.SECOND, MeasurementUnit.MINUTE, MeasurementUnit.HOUR);
		Set<MeasurementUnit> distance = EnumSet.of(
				MeasurementUnit.METER, MeasurementUnit.KILOMETER, MeasurementUnit.MILE);
		Set<MeasurementUnit> strength = EnumSet.of(MeasurementUnit.KILOGRAM, MeasurementUnit.POUND);
		Set<MeasurementUnit> score = EnumSet.of(MeasurementUnit.SCORE);
		Set<MeasurementUnit> flexibility = EnumSet.of(
				MeasurementUnit.CENTIMETER, MeasurementUnit.INCH, MeasurementUnit.SCORE);

		map.put(MeasurementType.BODY_WEIGHT, mass);
		map.put(MeasurementType.LEAN_BODY_MASS, mass);
		map.put(MeasurementType.BODY_FAT_PERCENTAGE, EnumSet.of(MeasurementUnit.PERCENT));
		map.put(MeasurementType.WAIST_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.CHEST_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.HIP_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.ARM_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.THIGH_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.CALF_CIRCUMFERENCE, circumference);
		map.put(MeasurementType.RESTING_HEART_RATE, heartRate);
		map.put(MeasurementType.MAX_HEART_RATE, heartRate);
		map.put(MeasurementType.BLOOD_PRESSURE_SYSTOLIC, bloodPressure);
		map.put(MeasurementType.BLOOD_PRESSURE_DIASTOLIC, bloodPressure);
		map.put(MeasurementType.VERTICAL_JUMP, jump);
		map.put(MeasurementType.BROAD_JUMP, jump);
		map.put(MeasurementType.SPRINT_TIME, time);
		map.put(MeasurementType.RUN_TIME, time);
		map.put(MeasurementType.SWIM_TIME, time);
		map.put(MeasurementType.CYCLING_TIME, time);
		map.put(MeasurementType.RUN_DISTANCE, distance);
		map.put(MeasurementType.SWIM_DISTANCE, distance);
		map.put(MeasurementType.CYCLING_DISTANCE, distance);
		map.put(MeasurementType.BENCH_PRESS, strength);
		map.put(MeasurementType.BACK_SQUAT, strength);
		map.put(MeasurementType.DEADLIFT, strength);
		map.put(MeasurementType.OVERHEAD_PRESS, strength);
		map.put(MeasurementType.POWER_CLEAN, strength);
		map.put(MeasurementType.REPETITION_MAX, EnumSet.of(MeasurementUnit.REPETITION));
		map.put(MeasurementType.VO2_MAX, EnumSet.of(MeasurementUnit.MILLILITER_PER_KILOGRAM_PER_MINUTE));
		map.put(MeasurementType.FLEXIBILITY, flexibility);
		map.put(MeasurementType.MOBILITY_SCORE, score);
		map.put(MeasurementType.SESSION_RPE, score);
		map.put(MeasurementType.SLEEP_DURATION, EnumSet.of(MeasurementUnit.MINUTE, MeasurementUnit.HOUR));
		map.put(MeasurementType.OTHER, EnumSet.allOf(MeasurementUnit.class));
		return Map.copyOf(map);
	}

}
