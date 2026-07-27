package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExerciseNameNormalizerTests {

	@Test
	void whitespaceIsTrimmedAndCollapsedAndCaseIsFolded() {
		assertThat(ExerciseNameNormalizer.normalize("  Back\tSquat\n ")).isEqualTo("back squat");
		assertThat(ExerciseNameNormalizer.normalize("BACK   squat")).isEqualTo("back squat");
	}

	@Test
	void compatibilityFormsFoldTogetherSoLookalikeNamesDoNotSplitHistory() {
		assertThat(ExerciseNameNormalizer.normalize("Ｂａｃｋ Ｓｑｕａｔ")).isEqualTo("back squat");
		assertThat(ExerciseNameNormalizer.normalize("Deadlift ①")).isEqualTo("deadlift 1");
	}

	@Test
	void punctuationIsPreservedBecauseItDistinguishesMovements() {
		assertThat(ExerciseNameNormalizer.normalize("Clean & Jerk")).isEqualTo("clean & jerk");
		assertThat(ExerciseNameNormalizer.normalize("Clean Jerk")).isNotEqualTo("clean & jerk");
		assertThat(ExerciseNameNormalizer.normalize("Single-Leg RDL")).isEqualTo("single-leg rdl");
	}

}
