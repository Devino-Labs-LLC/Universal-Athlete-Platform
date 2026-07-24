package com.devinolabs.uap.identity.application;

import java.util.List;

import com.devinolabs.uap.identity.domain.PasswordPolicyViolation;

public class PasswordPolicyViolationException extends RuntimeException {

	private final List<PasswordPolicyViolation> violations;

	public PasswordPolicyViolationException(List<PasswordPolicyViolation> violations) {
		super("Password does not meet policy requirements: " + violations);
		this.violations = List.copyOf(violations);
	}

	public List<PasswordPolicyViolation> violations() {
		return violations;
	}

}
