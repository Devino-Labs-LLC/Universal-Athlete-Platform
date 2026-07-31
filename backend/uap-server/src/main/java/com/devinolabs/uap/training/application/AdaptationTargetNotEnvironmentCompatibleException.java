package com.devinolabs.uap.training.application;

public class AdaptationTargetNotEnvironmentCompatibleException extends RuntimeException {

	public AdaptationTargetNotEnvironmentCompatibleException() {
		this("Adaptation target is not compatible with the proposal environment");
	}

	public AdaptationTargetNotEnvironmentCompatibleException(String message) {
		super(message);
	}

}
