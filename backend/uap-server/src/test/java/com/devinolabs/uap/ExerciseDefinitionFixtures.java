package com.devinolabs.uap;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import com.devinolabs.uap.training.application.CreateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseNameNormalizer;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;

/**
 * Resolves the exercise definition a test needs from the movement name it already uses.
 *
 * <p>Prescriptions now require a canonical definition, and most tests only care that one exists.
 * Names that match a V19 system seed resolve to that seed; anything else becomes a custom definition
 * for the athlete, created once per athlete and name so repeated lookups are stable.
 */
public class ExerciseDefinitionFixtures {

	private static final Map<String, ExerciseDefinitionId> SYSTEM_BY_NORMALIZED_NAME = Map.of(
			"back squat", SystemExerciseDefinitions.BACK_SQUAT,
			"front squat", SystemExerciseDefinitions.FRONT_SQUAT,
			"bench press", SystemExerciseDefinitions.BENCH_PRESS,
			"romanian deadlift", SystemExerciseDefinitions.ROMANIAN_DEADLIFT,
			"running", SystemExerciseDefinitions.RUNNING,
			"cycling", SystemExerciseDefinitions.CYCLING,
			"plank", SystemExerciseDefinitions.PLANK,
			"box jump", SystemExerciseDefinitions.BOX_JUMP,
			"goblet squat", SystemExerciseDefinitions.GOBLET_SQUAT,
			"leg press", SystemExerciseDefinitions.LEG_PRESS);

	private final CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	private final Map<String, ExerciseDefinitionId> customByAthleteAndName = new ConcurrentHashMap<>();

	public ExerciseDefinitionFixtures(CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase) {
		this.createAthleteExerciseDefinitionUseCase = Objects.requireNonNull(createAthleteExerciseDefinitionUseCase);
	}

	public ExerciseDefinitionId idFor(AccountId accountId, String canonicalName) {
		String normalized = ExerciseNameNormalizer.normalize(canonicalName);
		ExerciseDefinitionId system = SYSTEM_BY_NORMALIZED_NAME.get(normalized);
		return system == null ? custom(accountId, canonicalName) : system;
	}

	public ExerciseDefinitionId custom(AccountId accountId, String canonicalName) {
		String key = accountId.value() + "|" + ExerciseNameNormalizer.normalize(canonicalName);
		return customByAthleteAndName.computeIfAbsent(
				key,
				ignored -> createAthleteExerciseDefinitionUseCase.execute(
						accountId, canonicalName, ExerciseDefinitionMetadataFixtures.defaultCustom()).id());
	}

}
