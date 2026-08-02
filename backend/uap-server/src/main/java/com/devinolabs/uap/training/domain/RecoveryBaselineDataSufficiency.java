package com.devinolabs.uap.training.domain;

/**
 * Data-volume label for a baseline window. Not a medical-confidence or clinical significance claim.
 */
public enum RecoveryBaselineDataSufficiency {

	INSUFFICIENT,
	LIMITED,
	SUFFICIENT;

	public static RecoveryBaselineDataSufficiency of(int observationCount) {
		if (observationCount <= 2) {
			return INSUFFICIENT;
		}
		if (observationCount <= 6) {
			return LIMITED;
		}
		return SUFFICIENT;
	}

}
