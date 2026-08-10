package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.util.List;

import com.devinolabs.uap.training.application.TrainingClientBootstrapResult;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.TrainingClientContractVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.WeightUnit;

record TrainingClientBootstrapResponse(
		TrainingClientContractVersion clientContractVersion,
		TrainingClientBootstrapFeaturesResponse features,
		TrainingClientBootstrapLimitsResponse limits,
		TrainingClientBootstrapUnitsResponse units,
		TrainingClientBootstrapRatingScalesResponse ratingScales) {

	static TrainingClientBootstrapResponse from(TrainingClientBootstrapResult result) {
		return new TrainingClientBootstrapResponse(
				result.clientContractVersion(),
				new TrainingClientBootstrapFeaturesResponse(
						result.features().readinessEnabled(),
						result.features().recommendationsEnabled(),
						result.features().adaptationEnabled(),
						result.features().recoveryEnabled(),
						result.features().trainingLoadEnabled(),
						result.features().environmentsEnabled()),
				new TrainingClientBootstrapLimitsResponse(
						result.limits().recoveryHistoryMaxDays(),
						result.limits().baselineWindows(),
						result.limits().readinessAlgorithmVersion(),
						result.limits().recommendationAlgorithmVersion(),
						result.limits().maxEnvironmentPageSize(),
						result.limits().maxHistoryRangeDays(),
						result.limits().recoveryCheckInMaxPastDays()),
				new TrainingClientBootstrapUnitsResponse(
						result.units().canonicalWeightUnit(),
						result.units().distanceUnit(),
						result.units().durationUnit(),
						result.units().trainingLoadUnit()),
				new TrainingClientBootstrapRatingScalesResponse(
						result.ratingScales().recoveryRatingMin(),
						result.ratingScales().recoveryRatingMax(),
						result.ratingScales().sessionRpeMin(),
						result.ratingScales().sessionRpeMax()));
	}

}

record TrainingClientBootstrapFeaturesResponse(
		boolean readinessEnabled,
		boolean recommendationsEnabled,
		boolean adaptationEnabled,
		boolean recoveryEnabled,
		boolean trainingLoadEnabled,
		boolean environmentsEnabled) {
}

record TrainingClientBootstrapLimitsResponse(
		int recoveryHistoryMaxDays,
		List<Integer> baselineWindows,
		ReadinessAlgorithmVersion readinessAlgorithmVersion,
		TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
		int maxEnvironmentPageSize,
		int maxHistoryRangeDays,
		int recoveryCheckInMaxPastDays) {
}

record TrainingClientBootstrapUnitsResponse(
		WeightUnit canonicalWeightUnit,
		DistanceUnit distanceUnit,
		String durationUnit,
		String trainingLoadUnit) {
}

record TrainingClientBootstrapRatingScalesResponse(
		int recoveryRatingMin,
		int recoveryRatingMax,
		BigDecimal sessionRpeMin,
		BigDecimal sessionRpeMax) {
}
