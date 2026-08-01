package com.devinolabs.uap.training.domain;

import java.util.Objects;

/** Athlete-reported mood observation. Not a medical or mental-health interpretation. */
public final class MoodRating {

	private final RecoveryRating rating;

	private MoodRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static MoodRating of(int value) {
		return new MoodRating(RecoveryRating.of(
				value,
				"mood",
				new InvalidMoodRatingException("mood must be between 1 and 5 inclusive")));
	}

	public int value() {
		return rating.value();
	}

	public String label() {
		return switch (rating.value()) {
			case 1 -> "VERY_LOW";
			case 2 -> "LOW";
			case 3 -> "NEUTRAL";
			case 4 -> "GOOD";
			case 5 -> "VERY_GOOD";
			default -> throw new IllegalStateException("Unexpected mood: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MoodRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
