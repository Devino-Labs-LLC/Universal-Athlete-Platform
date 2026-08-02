package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RecoveryMetricTrendPointResult(
		LocalDate date,
		UUID checkInId,
		BigDecimal value,
		BigDecimal rollingAverage3,
		BigDecimal rollingAverage7,
		RecoveryTrainingLoadContextResult trainingLoadContext) {

}
