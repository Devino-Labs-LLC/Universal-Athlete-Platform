package com.devinolabs.uap.sponsorship.api;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class PartnerPresentationRules {

	private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");

	private PartnerPresentationRules() {
	}

	public static String requirePlain(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		String trimmed = value.trim();
		if (trimmed.length() > maxLength) {
			throw new IllegalArgumentException(field + " is too long");
		}
		if (containsMarkup(trimmed)) {
			throw new IllegalArgumentException(field + " must be plain text");
		}
		return trimmed;
	}

	public static String optionalPlain(String value, String field, int maxLength) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return requirePlain(value, field, maxLength);
	}

	public static String requireHttps(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return parseHttps(value.trim(), field);
	}

	public static String optionalHttps(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return parseHttps(value.trim(), field);
	}

	public static boolean containsMarkup(String value) {
		String lower = value.toLowerCase(Locale.ROOT);
		return value.indexOf('<') >= 0
				|| value.indexOf('>') >= 0
				|| lower.contains("javascript:")
				|| lower.contains("onerror=")
				|| lower.contains("onload=");
	}

	private static String parseHttps(String value, String field) {
		URI uri;
		try {
			uri = URI.create(value);
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException(field + " is not a valid URL");
		}
		if (uri.getScheme() == null || !"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
			throw new IllegalArgumentException(field + " must use HTTPS");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw new IllegalArgumentException(field + " is missing a host");
		}
		String host = uri.getHost().toLowerCase(Locale.ROOT);
		if (BLOCKED_HOSTS.contains(host) || host.endsWith(".local")) {
			throw new IllegalArgumentException(field + " host is not allowed");
		}
		if (uri.getUserInfo() != null) {
			throw new IllegalArgumentException(field + " must not include credentials");
		}
		if (uri.getRawQuery() != null && uri.getRawQuery().toLowerCase(Locale.ROOT).contains("javascript:")) {
			throw new IllegalArgumentException(field + " is not a valid URL");
		}
		return uri.toString();
	}
}
