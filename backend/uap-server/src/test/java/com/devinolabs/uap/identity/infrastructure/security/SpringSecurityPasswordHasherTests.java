package com.devinolabs.uap.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;

class SpringSecurityPasswordHasherTests {

	private final PasswordHasher passwordHasher = new SpringSecurityPasswordHasher();

	@Test
	void hashesPasswordWithoutRetainingPlaintext() {
		String rawPassword = "S3cure-Pass!";

		String hash = passwordHasher.hash(rawPassword);

		assertThat(hash).isNotBlank();
		assertThat(hash).isNotEqualTo(rawPassword);
		assertThat(hash).startsWith("$2a$");
	}

	@Test
	void matchesValidPasswordAndRejectsInvalidPassword() {
		String rawPassword = "S3cure-Pass!";
		PasswordCredential credential = PasswordCredential.fromHash(passwordHasher.hash(rawPassword));

		assertThat(passwordHasher.matches(rawPassword, credential)).isTrue();
		assertThat(passwordHasher.matches("wrong-password", credential)).isFalse();
	}

	@Test
	void rejectsBlankRawPasswordWhenHashing() {
		assertThatThrownBy(() -> passwordHasher.hash(" "))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> passwordHasher.hash(""))
				.isInstanceOf(IllegalArgumentException.class);
	}

}
