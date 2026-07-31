package com.devinolabs.uap.training.domain;

/**
 * Provenance of a session effort record. The system never invents session RPE; {@link #SYSTEM_DERIVED}
 * applies only to duration derivation, not RPE fabrication.
 */
public enum SessionEffortSource {

	ATHLETE_REPORTED,
	COACH_RECORDED,
	SYSTEM_DERIVED

}
