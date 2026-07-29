package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class EquipmentCompatibilityEvaluatorTests {

	@Test
	void emptyRequiredEquipmentIsAlwaysCompatible() {
		assertThat(EquipmentCompatibilityEvaluator.isCompatible(List.of(), List.of(EquipmentType.DUMBBELL)))
				.isTrue();
	}

	@Test
	void bodyweightRequiredEquipmentDoesNotRequireAvailability() {
		assertThat(EquipmentCompatibilityEvaluator.isCompatible(
				List.of(EquipmentType.BODYWEIGHT),
				List.of()))
				.isTrue();
	}

	@Test
	void allNonBodyweightRequiredItemsMustBeAvailable() {
		assertThat(EquipmentCompatibilityEvaluator.isCompatible(
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH)))
				.isTrue();
		assertThat(EquipmentCompatibilityEvaluator.isCompatible(
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				List.of(EquipmentType.DUMBBELL)))
				.isFalse();
	}

}
