package com.devinolabs.uap.organization.domain;

final class OrganizationNames {

	private static final int MAX_LENGTH = 200;

	private OrganizationNames() {
	}

	static String requireDisplayName(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("name must not be blank");
		}
		String normalized = value.trim();
		if (normalized.length() > MAX_LENGTH) {
			throw new IllegalArgumentException("name must not exceed 200 characters");
		}
		return normalized;
	}

}
