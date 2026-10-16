package com.devinolabs.uap.integrations.domain;

import java.util.Locale;

/**
 * Bounded quality for connected evidence (ADR-055). Not a medical-grade claim.
 */
public enum EvidenceQuality {
	HIGH,
	MEDIUM,
	LOW,
	UNKNOWN;

	public static EvidenceQuality parseOrUnknown(String raw) {
		if (raw == null || raw.isBlank()) {
			return UNKNOWN;
		}
		try {
			return EvidenceQuality.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			return UNKNOWN;
		}
	}

	public static EvidenceQuality requireParse(String raw) {
		if (raw == null || raw.isBlank()) {
			return UNKNOWN;
		}
		try {
			return EvidenceQuality.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown evidence quality: " + raw);
		}
	}

	public boolean meetsReadinessGate() {
		return this == HIGH || this == MEDIUM;
	}
}
