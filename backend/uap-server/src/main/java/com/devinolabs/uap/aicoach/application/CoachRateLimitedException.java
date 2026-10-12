package com.devinolabs.uap.aicoach.application;

public class CoachRateLimitedException extends RuntimeException {

	public CoachRateLimitedException() {
		super("Coach rate limit exceeded");
	}

}
