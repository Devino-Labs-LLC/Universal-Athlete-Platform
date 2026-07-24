package com.devinolabs.uap.identity.domain;

import java.util.List;
import java.util.Objects;

public final class PasswordPolicyResult {

	private final List<PasswordPolicyViolation> violations;

	private PasswordPolicyResult(List<PasswordPolicyViolation> violations) {
		this.violations = List.copyOf(Objects.requireNonNull(violations, "violations must not be null"));
	}

	public static PasswordPolicyResult valid() {
		return new PasswordPolicyResult(List.of());
	}

	public static PasswordPolicyResult invalid(List<PasswordPolicyViolation> violations) {
		if (violations == null || violations.isEmpty()) {
			throw new IllegalArgumentException("Invalid password policy result requires at least one violation");
		}
		return new PasswordPolicyResult(violations);
	}

	public boolean isValid() {
		return violations.isEmpty();
	}

	public List<PasswordPolicyViolation> violations() {
		return violations;
	}

}
