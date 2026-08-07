package com.devinolabs.uap.training.application;

public class DailyTrainingRecommendationCalculationFailedException extends RuntimeException {

	public DailyTrainingRecommendationCalculationFailedException(String message) {
		super(message);
	}

	public DailyTrainingRecommendationCalculationFailedException(String message, Throwable cause) {
		super(message, cause);
	}

}
