package com.devinolabs.uap.training.domain;

/**
 * Stable formula version for occurrence load summaries. Not derived from application build numbers.
 */
public enum TrainingLoadCalculationVersion {

	V1;

	public String persistenceValue() {
		return "TRAINING_LOAD_" + name();
	}

	public static TrainingLoadCalculationVersion current() {
		return V1;
	}

	public static TrainingLoadCalculationVersion fromPersistence(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("calculationVersion must not be blank");
		}
		String normalized = value.trim();
		if (normalized.equals("TRAINING_LOAD_V1") || normalized.equals("V1")) {
			return V1;
		}
		throw new IllegalArgumentException("Unknown calculation version: " + value);
	}

}
