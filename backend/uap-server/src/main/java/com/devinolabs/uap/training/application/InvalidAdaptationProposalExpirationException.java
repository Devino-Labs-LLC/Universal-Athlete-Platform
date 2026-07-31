package com.devinolabs.uap.training.application;

public class InvalidAdaptationProposalExpirationException extends RuntimeException {

	public InvalidAdaptationProposalExpirationException() {
		this("Adaptation proposal expiration must be between 5 and 1440 minutes");
	}

	public InvalidAdaptationProposalExpirationException(String message) {
		super(message);
	}

}
