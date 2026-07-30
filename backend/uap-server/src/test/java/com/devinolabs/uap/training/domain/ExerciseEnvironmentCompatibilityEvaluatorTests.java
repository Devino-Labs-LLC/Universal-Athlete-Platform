package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class ExerciseEnvironmentCompatibilityEvaluatorTests {

	@Test
	void reportsMissingRequiredEquipmentDeterministically() {
		ExerciseEnvironmentCompatibility compatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				List.of(EquipmentType.BARBELL, EquipmentType.SQUAT_RACK),
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH));

		assertThat(compatibility.compatible()).isFalse();
		assertThat(compatibility.missingRequiredEquipment()).containsExactly(
				EquipmentType.BARBELL,
				EquipmentType.SQUAT_RACK);
	}

	@Test
	void bodyweightRequiredEquipmentIsNeverMissing() {
		ExerciseEnvironmentCompatibility compatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				List.of(EquipmentType.BODYWEIGHT),
				List.of());

		assertThat(compatibility.compatible()).isTrue();
		assertThat(compatibility.missingRequiredEquipment()).isEmpty();
	}

	@Test
	void emptyRequiredEquipmentIsCompatibleWithAnyEnvironment() {
		ExerciseEnvironmentCompatibility compatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				List.of(),
				List.of(EquipmentType.DUMBBELL));

		assertThat(compatibility.compatible()).isTrue();
		assertThat(compatibility.missingRequiredEquipment()).isEmpty();
	}

}
