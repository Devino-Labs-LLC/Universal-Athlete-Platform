package com.devinolabs.uap.training.application;

public class TrainingMetricsRequireCompletedExecutionException extends RuntimeException {

	public TrainingMetricsRequireCompletedExecutionException() {
		super("Training performance metrics require a COMPLETED workout exercise execution");
	}

}
