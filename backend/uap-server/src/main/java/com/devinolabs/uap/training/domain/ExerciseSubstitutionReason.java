package com.devinolabs.uap.training.domain;

/**
 * Why the athlete performed a movement other than the one their plan prescribed.
 *
 * <p>The reason is captured per substitution event so the substitution log explains a deviation
 * without the athlete having to reconstruct it from memory later.
 */
public enum ExerciseSubstitutionReason {

	INJURY,
	PAIN_OR_DISCOMFORT,
	EQUIPMENT_UNAVAILABLE,
	FACILITY_CONSTRAINT,
	TIME_CONSTRAINT,
	FATIGUE_MANAGEMENT,
	TECHNIQUE_FOCUS,
	COACH_DIRECTIVE,
	ATHLETE_PREFERENCE,
	/**
	 * Recorded on the history entry written when a substitution is undone; it is never stored as
	 * the current reason on an execution, which by then performs its prescribed movement again.
	 */
	REVERSION,
	OTHER

}
