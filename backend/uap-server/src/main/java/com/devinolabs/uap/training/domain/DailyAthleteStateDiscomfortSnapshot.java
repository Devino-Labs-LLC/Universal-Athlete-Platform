package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public record DailyAthleteStateDiscomfortSnapshot(
		UUID id,
		BodyArea bodyArea,
		BodySide bodySide,
		int intensity,
		String notes,
		int orderIndex) {

	public DailyAthleteStateDiscomfortSnapshot {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(bodyArea, "bodyArea must not be null");
		Objects.requireNonNull(bodySide, "bodySide must not be null");
		if (intensity < 1 || intensity > 5) {
			throw new IllegalArgumentException("intensity must be between 1 and 5");
		}
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
	}

}
