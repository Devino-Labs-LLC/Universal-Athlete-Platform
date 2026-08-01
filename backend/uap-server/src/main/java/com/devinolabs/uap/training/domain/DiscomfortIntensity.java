package com.devinolabs.uap.training.domain;

import java.util.Objects;

/**
 * Athlete-reported discomfort intensity (1–5). Not a clinical pain scale and does not gate exercise.
 */
public final class DiscomfortIntensity {

	private final RecoveryRating rating;

	private DiscomfortIntensity(RecoveryRating rating) {
		this.rating = rating;
	}

	public static DiscomfortIntensity of(int value) {
		return new DiscomfortIntensity(RecoveryRating.of(
				value,
				"intensity",
				new InvalidDiscomfortIntensityException("intensity must be between 1 and 5 inclusive")));
	}

	public int value() {
		return rating.value();
	}

	public String label() {
		return switch (rating.value()) {
			case 1 -> "MINIMAL";
			case 2 -> "MILD";
			case 3 -> "MODERATE";
			case 4 -> "HIGH";
			case 5 -> "VERY_HIGH";
			default -> throw new IllegalStateException("Unexpected discomfort intensity: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof DiscomfortIntensity that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
