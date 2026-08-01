package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Athlete-reported muscle soreness observation. Not a diagnosis or injury inference.
 */
public final class MuscleSorenessRating {

	private final RecoveryRating rating;

	private MuscleSorenessRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static MuscleSorenessRating of(int value) {
		return new MuscleSorenessRating(RecoveryRating.of(
				value,
				"muscleSoreness",
				new InvalidMuscleSorenessRatingException(
						"muscleSoreness must be between 1 and 5 inclusive")));
	}

	public int value() {
		return rating.value();
	}

	public String label() {
		return switch (rating.value()) {
			case 1 -> "NONE_OR_MINIMAL";
			case 2 -> "MILD";
			case 3 -> "MODERATE";
			case 4 -> "HIGH";
			case 5 -> "VERY_HIGH";
			default -> throw new IllegalStateException("Unexpected muscle soreness: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MuscleSorenessRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
