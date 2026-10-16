package com.devinolabs.uap.integrations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EvidenceQualityPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");

	@Test
	void staleHeartRateIsLowRegardlessOfProvenance() {
		EvidenceQuality quality = EvidenceQualityPolicy.derive(
				ProvenanceClass.DIRECT_SENSOR,
				EvidenceQuality.HIGH,
				SignalFamily.HEART,
				NOW.minus(java.time.Duration.ofHours(30)),
				NOW,
				Set.of(DeviceCapability.HEART_RATE));
		assertThat(quality).isEqualTo(EvidenceQuality.LOW);
		assertThat(quality.meetsReadinessGate()).isFalse();
	}

	@Test
	void capabilityMismatchIsLow() {
		EvidenceQuality quality = EvidenceQualityPolicy.derive(
				ProvenanceClass.CLIENT_DEVICE,
				EvidenceQuality.HIGH,
				SignalFamily.HRV,
				NOW.minusSeconds(60),
				NOW,
				Set.of(DeviceCapability.HEART_RATE));
		assertThat(quality).isEqualTo(EvidenceQuality.LOW);
	}

	@Test
	void freshDirectSensorIsHigh() {
		EvidenceQuality quality = EvidenceQualityPolicy.derive(
				ProvenanceClass.DIRECT_SENSOR,
				EvidenceQuality.UNKNOWN,
				SignalFamily.HEART,
				NOW.minusSeconds(30),
				NOW,
				Set.of(DeviceCapability.HEART_RATE));
		assertThat(quality).isEqualTo(EvidenceQuality.HIGH);
	}
}
