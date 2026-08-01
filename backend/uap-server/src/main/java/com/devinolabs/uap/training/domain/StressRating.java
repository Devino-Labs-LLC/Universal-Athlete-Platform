package com.devinolabs.uap.training.domain;

import java.util.Objects;

/** Athlete-reported stress observation. Not a clinical mental-health assessment. */
public final class StressRating {

	private final RecoveryRating rating;

	private StressRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static StressRating of(int value) {
		return new StressRating(RecoveryRating.of(
				value,
				"stress",
				new InvalidStressRatingException("stress must be between 1 and 5 inclusive")));
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
			default -> throw new IllegalStateException("Unexpected stress: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof StressRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
