package com.devinolabs.uap.sponsorship.application;

public final class SponsorCopy {

	private SponsorCopy() {
	}

	public static String requirePlain(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new SponsorInputInvalidException(field + " is required");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new SponsorInputInvalidException(field + " is too long");
		}
		if (containsMarkup(trimmed)) {
			throw new SponsorInputInvalidException(field + " must be plain text");
		}
		return trimmed;
	}

	public static String optionalPlain(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return requirePlain(value, field, maxLength);
	}

	static boolean containsMarkup(String value) {
		String lower = value.toLowerCase();
		return value.indexOf('<') >= 0
				|| value.indexOf('>') >= 0
				|| lower.contains("javascript:")
				|| lower.contains("onerror=")
				|| lower.contains("onload=");
	}
}
