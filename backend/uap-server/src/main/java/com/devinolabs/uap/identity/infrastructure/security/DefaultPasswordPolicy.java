package com.devinolabs.uap.identity.infrastructure.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.identity.domain.PasswordPolicy;
import com.devinolabs.uap.identity.domain.PasswordPolicyResult;
import com.devinolabs.uap.identity.domain.PasswordPolicyViolation;

@Component
class DefaultPasswordPolicy implements PasswordPolicy {

	static final int MIN_LENGTH = 12;
	static final int MAX_LENGTH = 128;

	@Override
	public PasswordPolicyResult validate(CharSequence rawPassword) {
		if (rawPassword == null || rawPassword.toString().isBlank()) {
			return PasswordPolicyResult.invalid(List.of(PasswordPolicyViolation.NULL_OR_BLANK));
		}

		List<PasswordPolicyViolation> violations = new ArrayList<>();
		int length = rawPassword.length();
		if (length < MIN_LENGTH) {
			violations.add(PasswordPolicyViolation.TOO_SHORT);
		}
		if (length > MAX_LENGTH) {
			violations.add(PasswordPolicyViolation.TOO_LONG);
		}

		boolean hasUpper = false;
		boolean hasLower = false;
		boolean hasDigit = false;
		boolean hasSpecial = false;
		for (int i = 0; i < length; i++) {
			char character = rawPassword.charAt(i);
			if (Character.isUpperCase(character)) {
				hasUpper = true;
			}
			else if (Character.isLowerCase(character)) {
				hasLower = true;
			}
			else if (Character.isDigit(character)) {
				hasDigit = true;
			}
			else {
				hasSpecial = true;
			}
		}

		if (!hasUpper) {
			violations.add(PasswordPolicyViolation.MISSING_UPPERCASE);
		}
		if (!hasLower) {
			violations.add(PasswordPolicyViolation.MISSING_LOWERCASE);
		}
		if (!hasDigit) {
			violations.add(PasswordPolicyViolation.MISSING_DIGIT);
		}
		if (!hasSpecial) {
			violations.add(PasswordPolicyViolation.MISSING_SPECIAL_CHARACTER);
		}

		return violations.isEmpty() ? PasswordPolicyResult.valid() : PasswordPolicyResult.invalid(violations);
	}

}
