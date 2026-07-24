package com.devinolabs.uap.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.devinolabs.uap.identity.domain.PasswordPolicy;
import com.devinolabs.uap.identity.domain.PasswordPolicyResult;
import com.devinolabs.uap.identity.domain.PasswordPolicyViolation;

class DefaultPasswordPolicyTests {

	private final PasswordPolicy passwordPolicy = new DefaultPasswordPolicy();

	@Test
	void acceptsPasswordMeetingAllRulesWithoutTrimming() {
		PasswordPolicyResult result = passwordPolicy.validate(" Valid Pass1! ");

		assertThat(result.isValid()).isTrue();
		assertThat(" Valid Pass1! ".length()).isEqualTo(14);
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	void rejectsNullOrBlankPasswords(String rawPassword) {
		PasswordPolicyResult result = passwordPolicy.validate(rawPassword);

		assertThat(result.isValid()).isFalse();
		assertThat(result.violations()).containsExactly(PasswordPolicyViolation.NULL_OR_BLANK);
	}

	@Test
	void reportsStructuredViolationsForWeakPasswords() {
		PasswordPolicyResult tooShort = passwordPolicy.validate("Ab1!");
		assertThat(tooShort.violations()).contains(PasswordPolicyViolation.TOO_SHORT);

		PasswordPolicyResult missingClasses = passwordPolicy.validate("alllowercase1!");
		assertThat(missingClasses.violations()).contains(PasswordPolicyViolation.MISSING_UPPERCASE);

		PasswordPolicyResult missingSpecial = passwordPolicy.validate("NoSpecialChar1");
		assertThat(missingSpecial.violations()).contains(PasswordPolicyViolation.MISSING_SPECIAL_CHARACTER);

		PasswordPolicyResult tooLong = passwordPolicy.validate("Aa1!" + "x".repeat(125));
		assertThat(tooLong.violations()).contains(PasswordPolicyViolation.TOO_LONG);
	}

}
