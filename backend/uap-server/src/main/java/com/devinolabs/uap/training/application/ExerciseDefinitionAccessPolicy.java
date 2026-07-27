package com.devinolabs.uap.training.application;

import java.util.Objects;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

/**
 * The single place that decides which exercise definitions an athlete may see and prescribe.
 */
public final class ExerciseDefinitionAccessPolicy {

	private ExerciseDefinitionAccessPolicy() {
	}

	/**
	 * Shared SYSTEM definitions and the athlete's own custom definitions, including archived ones
	 * so an athlete can still read the identity behind their historical results.
	 */
	public static boolean isAccessible(AthleteId athleteId, ExerciseDefinition definition) {
		Objects.requireNonNull(athleteId, "athleteId must not be null");
		Objects.requireNonNull(definition, "definition must not be null");
		return definition.scope() == ExerciseDefinitionScope.SYSTEM || definition.isOwnedBy(athleteId);
	}

	public static boolean isSelectableForPrescription(AthleteId athleteId, ExerciseDefinition definition) {
		return isAccessible(athleteId, definition) && definition.active();
	}

	/**
	 * Resolves a definition for prescription, distinguishing "you cannot see this" from "you can
	 * see it but it is retired" so the caller gets an actionable error.
	 */
	public static ExerciseDefinition requireSelectable(AthleteId athleteId, ExerciseDefinition definition) {
		if (!isAccessible(athleteId, definition)) {
			throw new ExerciseDefinitionNotAccessibleException();
		}
		if (!definition.active()) {
			throw new ExerciseDefinitionArchivedException();
		}
		return definition;
	}

}
