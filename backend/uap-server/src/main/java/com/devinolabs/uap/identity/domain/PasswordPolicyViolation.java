package com.devinolabs.uap.identity.domain;

public enum PasswordPolicyViolation {

	NULL_OR_BLANK,
	TOO_SHORT,
	TOO_LONG,
	MISSING_UPPERCASE,
	MISSING_LOWERCASE,
	MISSING_DIGIT,
	MISSING_SPECIAL_CHARACTER

}
