package com.devinolabs.uap.training.application;

public record WorkoutExerciseSetCounts(
		int setCount,
		int notStartedSetCount,
		int inProgressSetCount,
		int completedSetCount,
		int skippedSetCount) {

	private static final WorkoutExerciseSetCounts NONE = new WorkoutExerciseSetCounts(0, 0, 0, 0, 0);

	public static WorkoutExerciseSetCounts none() {
		return NONE;
	}

}
