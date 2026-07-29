package com.devinolabs.uap.training.domain;

/**
 * Catalogue classification for an {@link ExerciseDefinition}.
 *
 * <p>Distinct from prescription-side {@link ExerciseCategory}, which describes how a movement is
 * programmed inside a workout day. Catalogue category is factual metadata about the movement itself.
 */
public enum ExerciseDefinitionCategory {

	STRENGTH,
	POWER,
	PLYOMETRIC,
	SPEED,
	AGILITY,
	ENDURANCE,
	MOBILITY,
	FLEXIBILITY,
	BALANCE,
	STABILITY,
	SKILL,
	RECOVERY,
	BREATHING,
	OTHER

}
