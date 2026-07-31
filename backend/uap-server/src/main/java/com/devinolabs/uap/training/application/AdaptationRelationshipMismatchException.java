package com.devinolabs.uap.training.application;

public class AdaptationRelationshipMismatchException extends RuntimeException {

	public AdaptationRelationshipMismatchException() {
		this("Adaptation substitution relationship mismatch");
	}

	public AdaptationRelationshipMismatchException(String message) {
		super(message);
	}

}
