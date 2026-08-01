package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Athlete-reported motivation to train. Does not authorize or prevent training.
 */
public final class TrainingMotivationRating {

	private final RecoveryRating rating;

	private TrainingMotivationRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static TrainingMotivationRating of(int value) {
		return new TrainingMotivationRating(RecoveryRating.of(
				value,
				"motivation",
				new InvalidTrainingMotivationRatingException(
						"motivation must be between 1 and 5 inclusive")));
	}

	public int value() {
		return rating.value();
	}

	public String label() {
		return switch (rating.value()) {
			case 1 -> "VERY_LOW";
			case 2 -> "LOW";
			case 3 -> "MODERATE";
			case 4 -> "HIGH";
			case 5 -> "VERY_HIGH";
			default -> throw new IllegalStateException("Unexpected motivation: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof TrainingMotivationRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
