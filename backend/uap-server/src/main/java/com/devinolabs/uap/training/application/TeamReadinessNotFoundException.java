package com.devinolabs.uap.training.application;

public class TeamReadinessNotFoundException extends RuntimeException {

	public TeamReadinessNotFoundException() {
		super("Team readiness was not found");
	}

}
