package com.devinolabs.uap.identity.domain;

import java.util.Locale;
import java.util.regex.Pattern;

public final class EmailAddress {

	private static final int MAX_LENGTH = 320;
	private static final Pattern EMAIL_PATTERN = Pattern.compile(
			"^[A-Za-z0-9_!#$%&'*+/=?`{|}~^.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

	private final String value;

	private EmailAddress(String value) {
		this.value = value;
	}

	public static EmailAddress of(String rawEmail) {
		if (rawEmail == null) {
			throw new IllegalArgumentException("Email address must not be null");
		}

		String normalized = rawEmail.trim().toLowerCase(Locale.ROOT);
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("Email address must not be blank");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("Email address must not exceed " + MAX_LENGTH + " characters");
		}
		if (!EMAIL_PATTERN.matcher(normalized).matches()) {
			throw new IllegalArgumentException("Email address is malformed");
		}

		return new EmailAddress(normalized);
	}

	public String value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof EmailAddress emailAddress)) {
			return false;
		}
		return value.equals(emailAddress.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value;
	}

}
