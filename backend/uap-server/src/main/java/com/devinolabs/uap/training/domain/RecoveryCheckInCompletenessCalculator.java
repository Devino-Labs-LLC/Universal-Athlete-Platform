package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Calculates {@link RecoveryCheckInCompleteness} from required wellness ratings.
 *
 * <p>Required: fatigue, muscleSoreness, stress, mood, motivation. Sleep fields remain optional.
 * Phase 7O create requires all five ratings, so create always yields COMPLETE.
 */
public final class RecoveryCheckInCompletenessCalculator {

	private RecoveryCheckInCompletenessCalculator() {
	}

	public static RecoveryCheckInCompleteness calculate(
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation) {
		if (fatigue == null || muscleSoreness == null || stress == null || mood == null || motivation == null) {
			if (allAbsent(fatigue, muscleSoreness, stress, mood, motivation)) {
				throw new EmptyRecoveryCheckInException(
						"At least one required wellness rating must be provided");
			}
			return RecoveryCheckInCompleteness.PARTIAL;
		}
		return RecoveryCheckInCompleteness.COMPLETE;
	}

	private static boolean allAbsent(
			FatigueRating fatigue,
			MuscleSorenessRating muscleSoreness,
			StressRating stress,
			MoodRating mood,
			TrainingMotivationRating motivation) {
		return fatigue == null
				&& muscleSoreness == null
				&& stress == null
				&& mood == null
				&& Objects.isNull(motivation);
	}

}
