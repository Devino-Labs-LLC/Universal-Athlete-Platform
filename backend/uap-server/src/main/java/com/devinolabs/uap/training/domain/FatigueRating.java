package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Athlete-perceived general fatigue (1–5). Not a calculated physiological fatigue score.
 */
public final class FatigueRating {

	private final RecoveryRating rating;

	private FatigueRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static FatigueRating of(int value) {
		return new FatigueRating(RecoveryRating.of(
				value,
				"fatigue",
				new InvalidFatigueRatingException("fatigue must be between 1 and 5 inclusive")));
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
			default -> throw new IllegalStateException("Unexpected fatigue: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof FatigueRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
