package com.devinolabs.uap.training.domain;

import java.util.List;

/**
 * Ids of the SYSTEM exercise definitions seeded by migrations V19 and V20.
 *
 * <p>The ids are fixed rather than generated so that seeded data, application code and tests can
 * refer to the same canonical movement without a lookup by name.
 */
public final class SystemExerciseDefinitions {

	public static final ExerciseDefinitionId BACK_SQUAT =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111101");

	public static final ExerciseDefinitionId FRONT_SQUAT =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111102");

	public static final ExerciseDefinitionId BENCH_PRESS =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111103");

	public static final ExerciseDefinitionId ROMANIAN_DEADLIFT =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111104");

	public static final ExerciseDefinitionId RUNNING =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111105");

	public static final ExerciseDefinitionId CYCLING =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111106");

	public static final ExerciseDefinitionId PLANK =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111107");

	public static final ExerciseDefinitionId BOX_JUMP =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111108");

	public static final ExerciseDefinitionId GOBLET_SQUAT =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111109");

	public static final ExerciseDefinitionId LEG_PRESS =
			ExerciseDefinitionId.of("11111111-1111-1111-1111-111111111110");

	private SystemExerciseDefinitions() {
	}

	public static List<ExerciseDefinitionId> all() {
		return List.of(
				BACK_SQUAT,
				FRONT_SQUAT,
				BENCH_PRESS,
				ROMANIAN_DEADLIFT,
				RUNNING,
				CYCLING,
				PLANK,
				BOX_JUMP,
				GOBLET_SQUAT,
				LEG_PRESS);
	}

}
