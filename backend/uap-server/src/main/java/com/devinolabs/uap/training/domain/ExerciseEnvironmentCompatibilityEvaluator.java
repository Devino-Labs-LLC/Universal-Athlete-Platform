package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Evaluates whether an environment's available equipment satisfies an exercise's required equipment.
 * Reuses {@link EquipmentCompatibilityEvaluator} semantics (BODYWEIGHT is not a missing external item).
 */
public final class ExerciseEnvironmentCompatibilityEvaluator {

	private ExerciseEnvironmentCompatibilityEvaluator() {
	}

	public static ExerciseEnvironmentCompatibility evaluate(
			Collection<EquipmentType> requiredEquipment,
			Collection<EquipmentType> availableEquipment) {
		Objects.requireNonNull(requiredEquipment, "requiredEquipment must not be null");
		List<EquipmentType> required = ordered(requiredEquipment);
		List<EquipmentType> available = ordered(availableEquipment == null ? List.of() : availableEquipment);
		Set<EquipmentType> availableSet = available.isEmpty()
				? EnumSet.noneOf(EquipmentType.class)
				: EnumSet.copyOf(available);
		List<EquipmentType> missing = new ArrayList<>();
		for (EquipmentType item : required) {
			if (item == EquipmentType.BODYWEIGHT) {
				continue;
			}
			if (!availableSet.contains(item)) {
				missing.add(item);
			}
		}
		missing.sort(Comparator.comparingInt(Enum::ordinal));
		boolean compatible = EquipmentCompatibilityEvaluator.isCompatible(required, available);
		return new ExerciseEnvironmentCompatibility(compatible, required, available, List.copyOf(missing));
	}

	private static List<EquipmentType> ordered(Collection<EquipmentType> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return values.stream().filter(Objects::nonNull).distinct()
				.sorted(Comparator.comparingInt(Enum::ordinal))
				.toList();
	}

}
