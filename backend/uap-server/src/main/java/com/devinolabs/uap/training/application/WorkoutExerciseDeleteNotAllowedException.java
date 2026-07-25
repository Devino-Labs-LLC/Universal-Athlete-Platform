package com.devinolabs.uap.training.application;

public class WorkoutExerciseDeleteNotAllowedException extends RuntimeException {

	public WorkoutExerciseDeleteNotAllowedException() {
		super("Only PLANNED workout exercises can be deleted");
	}

}
