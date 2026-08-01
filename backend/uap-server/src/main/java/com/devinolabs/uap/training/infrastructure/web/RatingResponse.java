package com.devinolabs.uap.training.infrastructure.web;

record RatingResponse(int value, String label) {

	static RatingResponse from(com.devinolabs.uap.training.domain.FatigueRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.MuscleSorenessRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.StressRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.MoodRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.TrainingMotivationRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.SleepQualityRating rating) {
		return new RatingResponse(rating.value(), rating.label());
	}

	static RatingResponse from(com.devinolabs.uap.training.domain.DiscomfortIntensity intensity) {
		return new RatingResponse(intensity.value(), intensity.label());
	}

}
