package com.devinolabs.uap.training.application;

public class InvalidFeasibilitySuggestionLimitException extends RuntimeException {

	public InvalidFeasibilitySuggestionLimitException() {
		super("suggestionLimit must be between 0 and 10");
	}

}
