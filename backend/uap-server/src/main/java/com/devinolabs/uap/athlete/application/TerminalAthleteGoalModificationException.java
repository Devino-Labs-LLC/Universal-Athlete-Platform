package com.devinolabs.uap.athlete.application;

public class TerminalAthleteGoalModificationException extends RuntimeException {

	public TerminalAthleteGoalModificationException() {
		super("Completed or cancelled goals must be reopened before editing");
	}

}
