package com.devinolabs.uap.training.application;

public record DailyAthleteStateFieldDifferenceResult(
		String field,
		String previousValue,
		String newValue) {
}
