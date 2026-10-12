package com.devinolabs.uap.sponsorship.application;

import java.net.URI;
import java.util.Locale;
import java.util.Set;

public final class SponsorDestination {

	private static final Set<String> BLOCKED_HOSTS = Set.of("localhost", "127.0.0.1", "0.0.0.0", "::1");

	private SponsorDestination() {
	}

	public static String requireHttps(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new SponsorInputInvalidException(field + " is required");
		}
		return parseHttps(value.trim(), field);
	}

	public static String optionalHttps(String value, String field) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return parseHttps(value.trim(), field);
	}

	private static String parseHttps(String value, String field) {
		URI uri;
		try {
			uri = URI.create(value);
		}
		catch (IllegalArgumentException ex) {
			throw new SponsorInputInvalidException(field + " is not a valid URL");
		}
		if (uri.getScheme() == null || !"https".equals(uri.getScheme().toLowerCase(Locale.ROOT))) {
			throw new SponsorInputInvalidException(field + " must use HTTPS");
		}
		if (uri.getHost() == null || uri.getHost().isBlank()) {
			throw new SponsorInputInvalidException(field + " is missing a host");
		}
		String host = uri.getHost().toLowerCase(Locale.ROOT);
		if (BLOCKED_HOSTS.contains(host) || host.endsWith(".local")) {
			throw new SponsorInputInvalidException(field + " host is not allowed");
		}
		if (uri.getUserInfo() != null) {
			throw new SponsorInputInvalidException(field + " must not include credentials");
		}
		if (uri.getRawQuery() != null && uri.getRawQuery().toLowerCase(Locale.ROOT).contains("javascript:")) {
			throw new SponsorInputInvalidException(field + " is not a valid URL");
		}
		return uri.toString();
	}
}
