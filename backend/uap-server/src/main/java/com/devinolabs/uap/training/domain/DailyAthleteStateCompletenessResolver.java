package com.devinolabs.uap.training.domain;

/**
 * Resolves source availability only.
 * <ul>
 *   <li>COMPLETE — recovery check-in present and baseline metrics assembled</li>
 *   <li>PARTIAL — no check-in, but training load and/or scheduled occurrences present</li>
 *   <li>MINIMAL — neither check-in nor training/schedule facts</li>
 * </ul>
 */
public final class DailyAthleteStateCompletenessResolver {

	private DailyAthleteStateCompletenessResolver() {
	}

	public static DailyAthleteStateCompleteness resolve(
			boolean checkInPresent,
			boolean hasTrainingLoad,
			boolean hasScheduledOccurrences) {
		if (checkInPresent) {
			return DailyAthleteStateCompleteness.COMPLETE;
		}
		if (hasTrainingLoad || hasScheduledOccurrences) {
			return DailyAthleteStateCompleteness.PARTIAL;
		}
		return DailyAthleteStateCompleteness.MINIMAL;
	}

}
