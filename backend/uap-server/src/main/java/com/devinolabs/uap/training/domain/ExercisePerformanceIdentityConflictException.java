package com.devinolabs.uap.training.domain;

/**
 * Raised when an execution's performance key and exercise definition disagree, which would split
 * or merge an athlete's history under the wrong identity.
 */
public class ExercisePerformanceIdentityConflictException extends RuntimeException {

	public ExercisePerformanceIdentityConflictException(String message) {
		super(message);
	}

}
