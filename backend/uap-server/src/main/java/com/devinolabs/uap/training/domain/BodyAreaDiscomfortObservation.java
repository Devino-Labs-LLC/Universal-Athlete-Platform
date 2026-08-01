package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Athlete-reported body-area discomfort observation. Not a diagnosis.
 */
public final class BodyAreaDiscomfortObservation {

	public static final int MAX_OBSERVATIONS = 20;
	public static final int MAX_NOTES_LENGTH = 250;

	private final BodyArea bodyArea;
	private final BodySide side;
	private final DiscomfortIntensity intensity;
	private final String notes;
	private final int orderIndex;

	private BodyAreaDiscomfortObservation(
			BodyArea bodyArea,
			BodySide side,
			DiscomfortIntensity intensity,
			String notes,
			int orderIndex) {
		this.bodyArea = Objects.requireNonNull(bodyArea, "bodyArea must not be null");
		this.side = Objects.requireNonNull(side, "side must not be null");
		this.intensity = Objects.requireNonNull(intensity, "intensity must not be null");
		this.notes = normalizeNotes(notes);
		if (orderIndex < 0) {
			throw new IllegalArgumentException("orderIndex must not be negative");
		}
		this.orderIndex = orderIndex;
		if (bodyArea == BodyArea.GENERAL_FULL_BODY && side != BodySide.NOT_APPLICABLE) {
			throw new InvalidBodyAreaDiscomfortException(
					"GENERAL_FULL_BODY discomfort must use NOT_APPLICABLE side");
		}
	}

	public static BodyAreaDiscomfortObservation of(
			BodyArea bodyArea,
			BodySide side,
			DiscomfortIntensity intensity,
			String notes,
			int orderIndex) {
		return new BodyAreaDiscomfortObservation(bodyArea, side, intensity, notes, orderIndex);
	}

	public static List<BodyAreaDiscomfortObservation> validateAndOrder(List<Input> inputs) {
		if (inputs == null || inputs.isEmpty()) {
			return List.of();
		}
		if (inputs.size() > MAX_OBSERVATIONS) {
			throw new TooManyBodyAreaDiscomfortObservationsException(
					"At most " + MAX_OBSERVATIONS + " discomfort observations are allowed");
		}
		Set<String> seen = new HashSet<>();
		List<BodyAreaDiscomfortObservation> observations = new ArrayList<>();
		for (Input input : inputs) {
			BodyArea area = parseBodyArea(input.bodyArea());
			BodySide side = parseBodySide(input.side());
			DiscomfortIntensity intensity = DiscomfortIntensity.of(input.intensity());
			String key = area.name() + ":" + side.name();
			if (!seen.add(key)) {
				throw new DuplicateBodyAreaDiscomfortException(
						"Duplicate discomfort for bodyArea " + area + " and side " + side);
			}
			observations.add(BodyAreaDiscomfortObservation.of(area, side, intensity, input.notes(), 0));
		}
		observations.sort(Comparator
				.comparing(BodyAreaDiscomfortObservation::bodyArea)
				.thenComparing(BodyAreaDiscomfortObservation::side));
		List<BodyAreaDiscomfortObservation> ordered = new ArrayList<>();
		for (int index = 0; index < observations.size(); index++) {
			BodyAreaDiscomfortObservation observation = observations.get(index);
			ordered.add(BodyAreaDiscomfortObservation.of(
					observation.bodyArea(),
					observation.side(),
					observation.intensity(),
					observation.notes(),
					index));
		}
		return List.copyOf(ordered);
	}

	public static BodyArea parseBodyArea(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidBodyAreaException("bodyArea is required");
		}
		try {
			return BodyArea.valueOf(value.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidBodyAreaException("Invalid bodyArea: " + value);
		}
	}

	public static BodySide parseBodySide(String value) {
		if (value == null || value.isBlank()) {
			throw new InvalidBodySideException("side is required");
		}
		try {
			return BodySide.valueOf(value.trim());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidBodySideException("Invalid side: " + value);
		}
	}

	static String normalizeNotes(String notes) {
		if (notes == null || notes.isBlank()) {
			return null;
		}
		String trimmed = notes.trim();
		if (trimmed.length() > MAX_NOTES_LENGTH) {
			throw new InvalidBodyAreaDiscomfortException(
					"discomfort notes must not exceed " + MAX_NOTES_LENGTH + " characters");
		}
		return trimmed;
	}

	public BodyArea bodyArea() {
		return bodyArea;
	}

	public BodySide side() {
		return side;
	}

	public DiscomfortIntensity intensity() {
		return intensity;
	}

	public String notes() {
		return notes;
	}

	public int orderIndex() {
		return orderIndex;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof BodyAreaDiscomfortObservation that)) {
			return false;
		}
		return bodyArea == that.bodyArea
				&& side == that.side
				&& Objects.equals(intensity, that.intensity)
				&& Objects.equals(notes, that.notes)
				&& orderIndex == that.orderIndex;
	}

	@Override
	public int hashCode() {
		return Objects.hash(bodyArea, side, intensity, notes, orderIndex);
	}

	public record Input(String bodyArea, String side, int intensity, String notes) {
	}

}
