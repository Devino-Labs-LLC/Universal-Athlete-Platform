package com.devinolabs.uap.integrations.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Deterministic fusion. Original evidence is not modified (ADR-055).
 */
public final class SensorFusionPolicy {

	public static final int PROVENANCE_DIRECT = 400;
	public static final int PROVENANCE_CLIENT = 350;
	public static final int PROVENANCE_OAUTH = 300;
	public static final int PROVENANCE_OS_HUB = 200;
	public static final int QUALITY_HIGH = 40;
	public static final int QUALITY_MEDIUM = 20;
	public static final int QUALITY_LOW = 5;
	public static final int FRESH_BONUS = 15;
	public static final int CAPABILITY_BONUS = 10;
	public static final Duration FRESH_WINDOW = Duration.ofHours(6);

	private SensorFusionPolicy() {
	}

	public record FusionCandidate(
			UUID evidenceId,
			UUID deviceId,
			SignalFamily family,
			String signalType,
			Instant observedAt,
			ProvenanceClass provenance,
			EvidenceQuality quality,
			Set<DeviceCapability> capabilities) {
	}

	public record FusionWinner(FusionCandidate winner, int score, List<UUID> consideredEvidenceIds) {
	}

	public static List<FusionWinner> fuse(List<FusionCandidate> candidates, Instant now) {
		Objects.requireNonNull(now, "now must not be null");
		Map<String, List<FusionCandidate>> groups = new LinkedHashMap<>();
		for (FusionCandidate candidate : candidates) {
			if (candidate == null || candidate.family() == null || candidate.signalType() == null
					|| candidate.observedAt() == null) {
				continue;
			}
			groups.computeIfAbsent(groupKey(candidate), key -> new ArrayList<>()).add(candidate);
		}
		List<FusionWinner> winners = new ArrayList<>();
		for (List<FusionCandidate> group : groups.values()) {
			winners.add(pick(group, now));
		}
		return List.copyOf(winners);
	}

	public static int score(FusionCandidate candidate, Instant now) {
		int total = provenanceScore(candidate.provenance());
		total += qualityScore(candidate.quality());
		if (candidate.observedAt() != null && !candidate.observedAt().isBefore(now.minus(FRESH_WINDOW))) {
			total += FRESH_BONUS;
		}
		Set<DeviceCapability> capabilities = candidate.capabilities();
		if (capabilities != null
				&& capabilities.stream().anyMatch(capability -> capability.supports(candidate.family()))) {
			total += CAPABILITY_BONUS;
		}
		return total;
	}

	private static FusionWinner pick(List<FusionCandidate> group, Instant now) {
		Comparator<FusionCandidate> order = Comparator
				.comparingInt((FusionCandidate candidate) -> score(candidate, now))
				.reversed()
				.thenComparing(FusionCandidate::observedAt, Comparator.reverseOrder())
				.thenComparing(candidate -> candidate.evidenceId().toString());
		FusionCandidate winner = group.stream().min(order).orElseThrow();
		List<UUID> ids = group.stream().map(FusionCandidate::evidenceId).toList();
		return new FusionWinner(winner, score(winner, now), ids);
	}

	private static String groupKey(FusionCandidate candidate) {
		LocalDate day = LocalDate.ofInstant(candidate.observedAt(), ZoneOffset.UTC);
		return candidate.family().name() + "|" + candidate.signalType().trim().toUpperCase() + "|" + day;
	}

	private static int provenanceScore(ProvenanceClass provenance) {
		if (provenance == null) {
			return 0;
		}
		return switch (provenance) {
			case DIRECT_SENSOR -> PROVENANCE_DIRECT;
			case CLIENT_DEVICE -> PROVENANCE_CLIENT;
			case OAUTH_PROVIDER -> PROVENANCE_OAUTH;
			case OS_HUB -> PROVENANCE_OS_HUB;
		};
	}

	private static int qualityScore(EvidenceQuality quality) {
		if (quality == null) {
			return 0;
		}
		return switch (quality) {
			case HIGH -> QUALITY_HIGH;
			case MEDIUM -> QUALITY_MEDIUM;
			case LOW -> QUALITY_LOW;
			case UNKNOWN -> 0;
		};
	}
}
