package com.devinolabs.uap.training.application;

public class ConflictingEquipmentContextFiltersException extends RuntimeException {

	public ConflictingEquipmentContextFiltersException() {
		super("Provide either trainingEnvironmentId or availableEquipment, not both");
	}

}
