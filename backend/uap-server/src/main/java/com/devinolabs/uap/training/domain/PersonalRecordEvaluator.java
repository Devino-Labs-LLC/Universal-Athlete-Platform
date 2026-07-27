package com.devinolabs.uap.training.domain;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Decides whether a candidate takes a personal record slot.
 *
 * <p>A strictly better normalized value always wins. An exact tie keeps the earlier achievement,
 * falling back to the lower set UUID when two ties share a timestamp, so the same inputs always
 * settle on the same winner no matter what order they are processed in.
 */
public final class PersonalRecordEvaluator {

	/**
	 * Deterministic ordering of candidates competing for the same slot: best value first, then
	 * earliest achievement, then lowest set UUID.
	 */
	public static final Comparator<PersonalRecordCandidate> PREFERENCE = Comparator
			.comparing(PersonalRecordCandidate::normalizedValue, Comparator.reverseOrder())
			.thenComparing(PersonalRecordCandidate::achievedAt)
			.thenComparing(candidate -> candidate.sourceSetId().value());

	private PersonalRecordEvaluator() {
	}

	public enum Outcome {

		/** No record existed for this slot; the candidate establishes one. */
		ESTABLISHED,

		/** The candidate strictly beats the standing record. */
		IMPROVED,

		/** The candidate ties the standing record but is the preferred provenance. */
		REPROVENANCED,

		/** The standing record survives. */
		UNCHANGED;

		/**
		 * Only a genuinely new best is worth an append-only history entry; ties are not.
		 */
		public boolean appendsHistory() {
			return this == ESTABLISHED || this == IMPROVED;
		}

		public boolean writesProjection() {
			return this != UNCHANGED;
		}

	}

	public static Outcome evaluate(PersonalRecordCandidate candidate, AthleteExercisePersonalRecord current) {
		Objects.requireNonNull(candidate, "candidate must not be null");
		if (current == null) {
			return Outcome.ESTABLISHED;
		}
		if (candidate.recordType() != current.recordType()
				|| !Objects.equals(candidate.recordQualifier(), current.recordQualifier())) {
			throw new IllegalArgumentException("Candidate does not belong to this personal record slot");
		}
		int byValue = candidate.normalizedValue().compareTo(current.normalizedValue());
		if (byValue > 0) {
			return Outcome.IMPROVED;
		}
		if (byValue < 0) {
			return Outcome.UNCHANGED;
		}
		return winsTieBreak(candidate, current.achievedAt(), current.sourceSetId())
				? Outcome.REPROVENANCED
				: Outcome.UNCHANGED;
	}

	/**
	 * Picks the single candidate that should represent a slot, applying the same rules used against
	 * a stored record.
	 */
	public static PersonalRecordCandidate best(List<PersonalRecordCandidate> candidates) {
		Objects.requireNonNull(candidates, "candidates must not be null");
		return candidates.stream().min(PREFERENCE).orElse(null);
	}

	private static boolean winsTieBreak(
			PersonalRecordCandidate candidate,
			Instant currentAchievedAt,
			WorkoutExerciseSetId currentSetId) {
		int byAchievedAt = candidate.achievedAt().compareTo(currentAchievedAt);
		if (byAchievedAt != 0) {
			return byAchievedAt < 0;
		}
		return candidate.sourceSetId().value().compareTo(currentSetId.value()) < 0;
	}

}
