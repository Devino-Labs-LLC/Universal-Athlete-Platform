package com.devinolabs.uap.training.domain;

/**
 * How the movement loads sides of the body.
 *
 * <p>Convention: cyclic locomotor activities such as Running and Cycling use
 * {@link #NOT_APPLICABLE} rather than {@link #ALTERNATING}, because laterality is not a programming
 * distinction for those catalogue entries.
 */
public enum ExerciseLaterality {

	BILATERAL,
	UNILATERAL,
	ALTERNATING,
	ASYMMETRICAL,
	NOT_APPLICABLE

}
