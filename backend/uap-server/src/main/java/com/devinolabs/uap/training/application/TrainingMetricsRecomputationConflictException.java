package com.devinolabs.uap.training.application;

public class TrainingMetricsRecomputationConflictException extends RuntimeException {

	public TrainingMetricsRecomputationConflictException(Throwable cause) {
		super("Training performance metrics were modified concurrently; retry the request", cause);
	}

}
