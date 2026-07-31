package com.devinolabs.uap.training.application;

public class AdaptationTargetNotAccessibleException extends RuntimeException {

	public AdaptationTargetNotAccessibleException() {
		this("Adaptation target is not accessible");
	}

	public AdaptationTargetNotAccessibleException(String message) {
		super(message);
	}

}
