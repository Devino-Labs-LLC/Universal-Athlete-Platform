package com.devinolabs.uap.training.domain;

public class TrainingEnvironmentArchivedException extends RuntimeException {

	public TrainingEnvironmentArchivedException() {
		super("Training environment is archived");
	}

}
