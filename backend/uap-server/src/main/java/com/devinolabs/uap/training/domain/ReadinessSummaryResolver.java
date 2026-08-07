package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic limiting/strongest dimension selection. No AI prose.
 */
public final class ReadinessSummaryResolver {

	private ReadinessSummaryResolver() {
	}

	public static List<ReadinessDimensionType> limiting(
			List<ReadinessDimensionContribution> contributions,
			int limit) {
		return contributions.stream()
				.filter(c -> c.normalizedScore() != null)
				.sorted(Comparator
						.comparing(ReadinessDimensionContribution::normalizedScore)
						.thenComparing(c -> c.dimensionType().name()))
				.limit(limit)
				.map(ReadinessDimensionContribution::dimensionType)
				.toList();
	}

	public static List<ReadinessDimensionType> strongest(
			List<ReadinessDimensionContribution> contributions,
			int limit) {
		return contributions.stream()
				.filter(c -> c.normalizedScore() != null)
				.sorted(Comparator
						.comparing(ReadinessDimensionContribution::normalizedScore).reversed()
						.thenComparing(c -> c.dimensionType().name()))
				.limit(limit)
				.map(ReadinessDimensionContribution::dimensionType)
				.toList();
	}

	public static List<ReadinessDimensionContribution> applyRanks(
			List<ReadinessDimensionContribution> contributions,
			List<ReadinessDimensionType> limiting,
			List<ReadinessDimensionType> strongest) {
		Objects.requireNonNull(contributions, "contributions must not be null");
		Map<ReadinessDimensionType, Integer> limitingRanks = ranks(limiting);
		Map<ReadinessDimensionType, Integer> strongestRanks = ranks(strongest);
		List<ReadinessDimensionContribution> ranked = new ArrayList<>(contributions.size());
		for (ReadinessDimensionContribution contribution : contributions) {
			ranked.add(contribution.withRanks(
					limitingRanks.get(contribution.dimensionType()),
					strongestRanks.get(contribution.dimensionType())));
		}
		return List.copyOf(ranked);
	}

	private static Map<ReadinessDimensionType, Integer> ranks(List<ReadinessDimensionType> ordered) {
		Map<ReadinessDimensionType, Integer> ranks = new HashMap<>();
		for (int index = 0; index < ordered.size(); index++) {
			ranks.put(ordered.get(index), index + 1);
		}
		return ranks;
	}

}
