package com.devinolabs.uap.training.domain;

/**
 * Assessment-level data sufficiency from usable core dimensions.
 * <ul>
 *   <li>INSUFFICIENT — fewer than 3 usable core recovery dimensions</li>
 *   <li>LIMITED — 3 or 4 usable core dimensions</li>
 *   <li>SUFFICIENT — all 5 core recovery dimensions usable</li>
 * </ul>
 */
public final class ReadinessDataSufficiencyResolver {

	private ReadinessDataSufficiencyResolver() {
	}

	public static ReadinessDataSufficiency resolve(long coreUsable) {
		if (coreUsable < ReadinessCalculator.MIN_CORE_USABLE_DIMENSIONS) {
			return ReadinessDataSufficiency.INSUFFICIENT;
		}
		if (coreUsable < 5) {
			return ReadinessDataSufficiency.LIMITED;
		}
		return ReadinessDataSufficiency.SUFFICIENT;
	}

}
