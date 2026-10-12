package com.devinolabs.uap.aicoach.application;

public class CoachConversationNotFoundException extends RuntimeException {

	public CoachConversationNotFoundException() {
		super("Coach conversation was not found");
	}

}
