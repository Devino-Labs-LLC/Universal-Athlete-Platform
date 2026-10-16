package com.devinolabs.uap.integrations.domain;

import java.util.Locale;
import java.util.Set;

/**
 * Canonical unit codes accepted at the integrations boundary (V5 plan §14 / V9).
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
			"KILOGRAM",
			"WATT",
			"RPM",
			"CELSIUS",
			"BREATH_PER_MIN");

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

	public static void assertUnitMatchesFamily(SignalFamily family, String unitCode) {
		String unit = normalizeAndRequire(unitCode);
		boolean ok = switch (family) {
			case HEART, HRV -> unit.equals("BPM") || unit.equals("MS");
			case SLEEP -> unit.equals("MINUTE") || unit.equals("SECOND");
			case ACTIVITY -> unit.equals("COUNT") || unit.equals("METER") || unit.equals("KCAL");
			case WORKOUT -> unit.equals("MINUTE") || unit.equals("SECOND") || unit.equals("METER")
					|| unit.equals("KCAL") || unit.equals("BPM");
			case POWER -> unit.equals("WATT");
			case CADENCE -> unit.equals("RPM") || unit.equals("COUNT");
			case RESPIRATORY -> unit.equals("BREATH_PER_MIN");
			case TEMPERATURE -> unit.equals("CELSIUS");
		};
		if (!ok) {
			throw new IllegalArgumentException("unitCode " + unit + " is not valid for " + family);
		}
	}
}
