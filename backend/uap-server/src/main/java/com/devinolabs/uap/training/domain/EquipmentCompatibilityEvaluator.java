package com.devinolabs.uap.training.domain;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Read-filter evaluator: a target is equipment-compatible when every required item is available.
 * Optional equipment is never required. Definitions with no required equipment are always compatible.
 *
 * <p>{@link EquipmentType#BODYWEIGHT} in required equipment means no external load is inherently
 * needed; it does not block optional equipment and does not require BODYWEIGHT to appear in the
 * available set.
 */
public final class EquipmentCompatibilityEvaluator {

	private EquipmentCompatibilityEvaluator() {
	}

	public static boolean isCompatible(
			Collection<EquipmentType> requiredEquipment,
			Collection<EquipmentType> availableEquipment) {
		Objects.requireNonNull(requiredEquipment, "requiredEquipment must not be null");
		if (requiredEquipment.isEmpty()) {
			return true;
		}
		Set<EquipmentType> available = availableEquipment == null || availableEquipment.isEmpty()
				? EnumSet.noneOf(EquipmentType.class)
				: EnumSet.copyOf(availableEquipment);
		for (EquipmentType required : requiredEquipment) {
			if (required == EquipmentType.BODYWEIGHT) {
				continue;
			}
			if (!available.contains(required)) {
				return false;
			}
		}
		return true;
	}

}
