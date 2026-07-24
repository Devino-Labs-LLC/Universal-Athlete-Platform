package com.devinolabs.uap.identity.domain;

import java.time.Duration;

public interface LockoutPolicy {

	int maxFailedAttempts();

	Duration lockDuration();

}
