package com.devinolabs.uap.training.domain;

import java.util.Objects;

public final class SleepQualityRating {

	private final RecoveryRating rating;

	private SleepQualityRating(RecoveryRating rating) {
		this.rating = rating;
	}

	public static SleepQualityRating of(int value) {
		return new SleepQualityRating(RecoveryRating.of(
				value,
				"sleepQuality",
				new InvalidSleepQualityException("sleepQuality must be between 1 and 5 inclusive")));
	}

	public int value() {
		return rating.value();
	}

	public String label() {
		return switch (rating.value()) {
			case 1 -> "VERY_POOR";
			case 2 -> "POOR";
			case 3 -> "FAIR";
			case 4 -> "GOOD";
			case 5 -> "EXCELLENT";
			default -> throw new IllegalStateException("Unexpected sleep quality: " + rating.value());
		};
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof SleepQualityRating that && Objects.equals(rating, that.rating);
	}

	@Override
	public int hashCode() {
		return rating.hashCode();
	}

}
