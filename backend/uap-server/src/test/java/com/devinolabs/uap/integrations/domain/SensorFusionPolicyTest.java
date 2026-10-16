package com.devinolabs.uap.integrations.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SensorFusionPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");

	@Test
	void directSensorBeatsOsHubOnSameDayAndPreservesLoserId() {
		UUID strap = UUID.fromString("11111111-1111-4111-8111-111111111111");
		UUID watch = UUID.fromString("22222222-2222-4222-8222-222222222222");
		var winner = SensorFusionPolicy.fuse(List.of(
				candidate(strap, ProvenanceClass.OS_HUB, EvidenceQuality.MEDIUM, NOW.minusSeconds(60)),
				candidate(watch, ProvenanceClass.DIRECT_SENSOR, EvidenceQuality.HIGH, NOW.minusSeconds(90))), NOW)
				.get(0);
		assertThat(winner.winner().evidenceId()).isEqualTo(watch);
		assertThat(winner.consideredEvidenceIds()).containsExactlyInAnyOrder(strap, watch);
	}

	@Test
	void equalProvenancePrefersNewerObservationDeterministically() {
		UUID older = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
		UUID newer = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
		var winner = SensorFusionPolicy.fuse(List.of(
				candidate(older, ProvenanceClass.CLIENT_DEVICE, EvidenceQuality.HIGH, NOW.minusSeconds(120)),
				candidate(newer, ProvenanceClass.CLIENT_DEVICE, EvidenceQuality.HIGH, NOW.minusSeconds(10))), NOW)
				.get(0);
		assertThat(winner.winner().evidenceId()).isEqualTo(newer);
	}

	private static SensorFusionPolicy.FusionCandidate candidate(
			UUID id,
			ProvenanceClass provenance,
			EvidenceQuality quality,
			Instant observedAt) {
		return new SensorFusionPolicy.FusionCandidate(
				id,
				null,
				SignalFamily.HEART,
				"HEART_RATE",
				observedAt,
				provenance,
				quality,
				Set.of(DeviceCapability.HEART_RATE));
	}
}
