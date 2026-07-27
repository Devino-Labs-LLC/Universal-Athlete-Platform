package com.devinolabs.uap.training.domain;

/**
 * The personal record dimensions derived from completed sets.
 *
 * <p>{@link #MOST_REPETITIONS_AT_WEIGHT} is the only qualified type: it keeps one record per
 * normalized weight bucket rather than a single overall best.
 */
public enum PersonalRecordType {

	HEAVIEST_WEIGHT(PersonalRecordMeasure.KILOGRAM, false),
	MOST_REPETITIONS(PersonalRecordMeasure.REPETITION, false),
	MOST_REPETITIONS_AT_WEIGHT(PersonalRecordMeasure.REPETITION, true),
	HIGHEST_ESTIMATED_ONE_REP_MAX(PersonalRecordMeasure.KILOGRAM, false),
	HIGHEST_SET_VOLUME(PersonalRecordMeasure.KILOGRAM_REPETITION, false),
	LONGEST_DURATION(PersonalRecordMeasure.SECOND, false),
	LONGEST_DISTANCE(PersonalRecordMeasure.METER, false);

	private final PersonalRecordMeasure normalizedMeasure;
	private final boolean qualified;

	PersonalRecordType(PersonalRecordMeasure normalizedMeasure, boolean qualified) {
		this.normalizedMeasure = normalizedMeasure;
		this.qualified = qualified;
	}

	public PersonalRecordMeasure normalizedMeasure() {
		return normalizedMeasure;
	}

	/**
	 * Whether the record is kept per qualifier bucket instead of a single overall best.
	 */
	public boolean qualified() {
		return qualified;
	}

	/**
	 * Whether the recorded value is derived from a formula rather than directly measured.
	 */
	public boolean estimated() {
		return this == HIGHEST_ESTIMATED_ONE_REP_MAX;
	}

}
