package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.List;

import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingClientContractVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.WeightUnit;

public record TrainingClientBootstrapResult(
		TrainingClientContractVersion clientContractVersion,
		Features features,
		Limits limits,
		Units units,
		RatingScales ratingScales) {

	public record Features(
			boolean readinessEnabled,
			boolean recommendationsEnabled,
			boolean adaptationEnabled,
			boolean recoveryEnabled,
			boolean trainingLoadEnabled,
			boolean environmentsEnabled) {
	}

	public record Limits(
			int recoveryHistoryMaxDays,
			List<Integer> baselineWindows,
			ReadinessAlgorithmVersion readinessAlgorithmVersion,
			TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
			int maxEnvironmentPageSize,
			int maxHistoryRangeDays,
			int recoveryCheckInMaxPastDays) {
	}

	public record Units(
			WeightUnit canonicalWeightUnit,
			DistanceUnit distanceUnit,
			String durationUnit,
			String trainingLoadUnit) {
	}

	public record RatingScales(
			int recoveryRatingMin,
			int recoveryRatingMax,
			BigDecimal sessionRpeMin,
			BigDecimal sessionRpeMax) {
	}

}
