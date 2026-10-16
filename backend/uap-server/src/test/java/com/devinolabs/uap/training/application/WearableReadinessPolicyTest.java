package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.integrations.api.evidence.FusedEvidenceView;
import com.devinolabs.uap.training.domain.ReadinessCalculator;

class WearableReadinessPolicyTest {

	private static final Instant NOW = Instant.parse("2026-10-12T18:00:00Z");

	@Test
	void duplicateFamiliesDoNotDoubleCountAndLowQualityIsIgnored() {
		FusedEvidenceView high = fused("HEART", "HIGH", NOW.minusSeconds(60));
		FusedEvidenceView duplicate = fused("HEART", "HIGH", NOW.minusSeconds(30));
		FusedEvidenceView low = fused("SLEEP", "LOW", NOW.minusSeconds(10));
		List<FusedEvidenceView> approved = WearableReadinessPolicy.approvedForReadiness(
				List.of(high, duplicate, low), NOW, false);
		assertThat(approved).hasSize(1);
		assertThat(approved.get(0).winningEvidenceId()).isEqualTo(high.winningEvidenceId());
	}

	@Test
	void athleteEnteredRecoveryWinsAndMissingWearableIsNotAPenalty() {
		assertThat(WearableReadinessPolicy.approvedForReadiness(
				List.of(fused("SLEEP", "HIGH", NOW)), NOW, true)).isEmpty();
		assertThat(WearableReadinessPolicy.approvedForReadiness(List.of(), NOW, false)).isEmpty();
		assertThat(WearableReadinessPolicy.missingWearableIsPenalty()).isFalse();
		assertThat(ReadinessCalculator.class.getDeclaredFields())
				.noneMatch(field -> field.getType().getPackageName().contains("integrations"));
	}

	@Test
	void staleEvidenceIsExcluded() {
		FusedEvidenceView stale = fused("HEART", "HIGH", NOW.minus(java.time.Duration.ofHours(30)));
		assertThat(WearableReadinessPolicy.approvedForReadiness(List.of(stale), NOW, false)).isEmpty();
	}

	private static FusedEvidenceView fused(String family, String quality, Instant observedAt) {
		return new FusedEvidenceView(
				UUID.randomUUID(),
				null,
				"APPLE_HEALTHKIT",
				family,
				family,
				new BigDecimal("60"),
				"BPM",
				observedAt,
				"OS_HUB",
				quality,
				200,
				List.of());
	}
}
