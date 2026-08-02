package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;

import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

record RecoveryMetricValueResponse(
		BigDecimal value,
		String label) {

	static RecoveryMetricValueResponse of(BigDecimal value, RecoveryMetricType metricType) {
		if (value == null) {
			return null;
		}
		String label = labelFor(metricType, value);
		return new RecoveryMetricValueResponse(value, label);
	}

	private static String labelFor(RecoveryMetricType metricType, BigDecimal value) {
		int intValue = value.intValue();
		return switch (metricType) {
			case SLEEP_DURATION -> value.stripTrailingZeros().toPlainString() + " min";
			case SLEEP_QUALITY -> SleepQualityRating.of(intValue).label();
			case FATIGUE -> FatigueRating.of(intValue).label();
			case MUSCLE_SORENESS -> MuscleSorenessRating.of(intValue).label();
			case STRESS -> StressRating.of(intValue).label();
			case MOOD -> MoodRating.of(intValue).label();
			case MOTIVATION -> TrainingMotivationRating.of(intValue).label();
		};
	}

}
