package com.devinolabs.uap.training.domain;

public enum WorkoutExerciseSetStatus {

	NOT_STARTED,
	IN_PROGRESS,
	COMPLETED,
	SKIPPED;

	public boolean isTerminal() {
		return this == COMPLETED || this == SKIPPED;
	}

	public boolean isActive() {
		return this == NOT_STARTED || this == IN_PROGRESS;
	}

}
