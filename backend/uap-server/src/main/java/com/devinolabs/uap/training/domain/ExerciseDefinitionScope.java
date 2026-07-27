package com.devinolabs.uap.training.domain;

/**
 * Who owns an exercise definition.
 *
 * <p>{@code SYSTEM} definitions are shared by every athlete and are never athlete owned;
 * {@code ATHLETE_CUSTOM} definitions belong to exactly one athlete and are invisible to others.
 */
public enum ExerciseDefinitionScope {

	SYSTEM,
	ATHLETE_CUSTOM

}
