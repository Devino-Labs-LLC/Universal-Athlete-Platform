package com.devinolabs.uap.training.application;

public class TrainingLoadRebuildFailedException extends RuntimeException {

	public TrainingLoadRebuildFailedException() {
		super("Training load rebuild failed");
	}

	public TrainingLoadRebuildFailedException(Throwable cause) {
		super("Training load rebuild failed", cause);
	}

}
