package com.devinolabs.uap.identity.infrastructure.security;

import java.util.Objects;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.identity.domain.PasswordCredential;
import com.devinolabs.uap.identity.domain.PasswordHasher;

@Component
class SpringSecurityPasswordHasher implements PasswordHasher {

	private final PasswordEncoder passwordEncoder;

	SpringSecurityPasswordHasher() {
		this(new BCryptPasswordEncoder());
	}

	SpringSecurityPasswordHasher(PasswordEncoder passwordEncoder) {
		this.passwordEncoder = Objects.requireNonNull(passwordEncoder, "PasswordEncoder must not be null");
	}

	@Override
	public String hash(CharSequence rawPassword) {
		if (rawPassword == null || rawPassword.toString().isBlank()) {
			throw new IllegalArgumentException("Raw password must not be blank");
		}
		return passwordEncoder.encode(rawPassword);
	}

	@Override
	public boolean matches(CharSequence rawPassword, PasswordCredential credential) {
		Objects.requireNonNull(credential, "Password credential must not be null");
		if (rawPassword == null || rawPassword.toString().isBlank()) {
			return false;
		}
		return passwordEncoder.matches(rawPassword, credential.hash());
	}

}
