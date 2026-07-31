package com.devinolabs.uap.training.application;

public class TrainingLoadRebuildConflictException extends RuntimeException {

	public TrainingLoadRebuildConflictException() {
		super("Training load rebuild conflict");
	}

	public TrainingLoadRebuildConflictException(Throwable cause) {
		super("Training load rebuild conflict", cause);
	}

}
