package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.UpdateTrainingEnvironmentCommand;

final class UpdateTrainingEnvironmentRequestMapper {
	private UpdateTrainingEnvironmentRequestMapper() {
	}

	static UpdateTrainingEnvironmentCommand toCommand(UpdateTrainingEnvironmentRequest request) {
		return new UpdateTrainingEnvironmentCommand(
				request.name() == null ? null : request.name().value(),
				request.name() != null,
				request.type() == null ? null : request.type().value(),
				request.type() != null,
				request.availableEquipment() == null ? null : request.availableEquipment().value(),
				request.availableEquipment() != null,
				request.description() == null ? null : request.description().value(),
				request.description() != null,
				request.facilityNotes() == null ? null : request.facilityNotes().value(),
				request.facilityNotes() != null,
				request.defaultEnvironment() == null ? null : request.defaultEnvironment().value(),
				request.defaultEnvironment() != null);
	}
}
