package com.devinolabs.uap.training.domain;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

/**
 * Produces the comparison form of an exercise name used for duplicate detection.
 *
 * <p>Punctuation is deliberately preserved: "Clean & Jerk" and "Clean Jerk" are different lifts,
 * so collapsing them would silently merge two athletes' histories.
 */
public final class ExerciseNameNormalizer {

	private ExerciseNameNormalizer() {
	}

	public static String normalize(String name) {
		Objects.requireNonNull(name, "name must not be null");
		String compatibilityForm = Normalizer.normalize(name.trim(), Normalizer.Form.NFKC);
		return compatibilityForm.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
	}

}
