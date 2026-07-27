package com.devinolabs.uap.training.application;

public class TrainingMetricsRequireCompletedSetsException extends RuntimeException {

	public TrainingMetricsRequireCompletedSetsException() {
		super("Training performance metrics require at least one COMPLETED set");
	}

}
