package com.devinolabs.uap.integrations.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Canonical unit codes accepted at the integrations boundary (V5 plan §14).
 * Adapters convert; provider-native unit strings do not leak.
 */
public final class EvidenceUnitCodes {

	private static final Set<String> ALL = Set.of(
			"MINUTE",
			"SECOND",
			"MS",
			"BPM",
			"KCAL",
			"METER",
			"COUNT",
			"KILOGRAM");

	private EvidenceUnitCodes() {
	}

	public static String normalizeAndRequire(String unitCode) {
		if (unitCode == null || unitCode.isBlank()) {
			throw new IllegalArgumentException("unitCode is required");
		}
		String normalized = unitCode.trim().toUpperCase(Locale.ROOT);
		if (!ALL.contains(normalized)) {
			throw new IllegalArgumentException("Unknown unit code: " + unitCode);
		}
		return normalized;
	}

	public static boolean isKnown(String unitCode) {
		if (unitCode == null || unitCode.isBlank()) {
			return false;
		}
		return ALL.contains(unitCode.trim().toUpperCase(Locale.ROOT));
	}
}
