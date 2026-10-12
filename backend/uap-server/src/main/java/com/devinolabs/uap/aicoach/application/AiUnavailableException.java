package com.devinolabs.uap.aicoach.application;

public class AiUnavailableException extends RuntimeException {

	public AiUnavailableException() {
		super("AI Coach is unavailable");
	}

}
