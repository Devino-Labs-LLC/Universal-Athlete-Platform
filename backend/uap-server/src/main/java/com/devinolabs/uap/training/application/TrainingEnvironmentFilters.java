package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

public record TrainingEnvironmentFilters(
		TrainingEnvironmentType type,
		List<EquipmentType> equipment,
		boolean activeOnly) {

	public TrainingEnvironmentFilters {
		equipment = equipment == null ? List.of() : List.copyOf(equipment);
	}

	public static TrainingEnvironmentFilters of(
			TrainingEnvironmentType type,
			List<EquipmentType> equipment,
			Boolean activeOnly) {
		return new TrainingEnvironmentFilters(type, equipment, activeOnly == null || activeOnly);
	}

}
