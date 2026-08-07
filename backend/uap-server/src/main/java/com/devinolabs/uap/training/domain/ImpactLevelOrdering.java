package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Explicit factual ordering for impact levels. Does not imply medical safety.
 */
public final class ImpactLevelOrdering {

	private ImpactLevelOrdering() {
	}

	public static int rank(ImpactLevel level) {
		return switch (Objects.requireNonNull(level, "level must not be null")) {
			case NO_IMPACT -> 0;
			case LOW_IMPACT -> 1;
			case MODERATE_IMPACT -> 2;
			case HIGH_IMPACT -> 3;
		};
	}

	/**
	 * Relative to source: lower impact first (0), same (1), higher (2), unknown (3).
	 */
	public static int relativePreferenceGroup(ImpactLevel source, ImpactLevel candidate) {
		if (source == null || candidate == null) {
			return 3;
		}
		int cmp = Integer.compare(rank(candidate), rank(source));
		if (cmp < 0) {
			return 0;
		}
		if (cmp == 0) {
			return 1;
		}
		return 2;
	}

}
