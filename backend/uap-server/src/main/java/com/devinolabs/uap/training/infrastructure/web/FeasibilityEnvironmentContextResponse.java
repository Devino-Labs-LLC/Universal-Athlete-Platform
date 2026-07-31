package com.devinolabs.uap.training.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.FeasibilityEnvironmentContextResult;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;

record FeasibilityEnvironmentContextResponse(
		UUID trainingEnvironmentId,
		String trainingEnvironmentName,
		List<EquipmentType> availableEquipment,
		FeasibilityEnvironmentContextSource contextSource,
		Instant snapshotCapturedAt) {

	static FeasibilityEnvironmentContextResponse from(FeasibilityEnvironmentContextResult result) {
		if (result == null) {
			return null;
		}
		return new FeasibilityEnvironmentContextResponse(
				result.trainingEnvironmentId() == null ? null : result.trainingEnvironmentId().value(),
				result.trainingEnvironmentName(),
				result.availableEquipment(),
				result.contextSource(),
				result.snapshotCapturedAt());
	}

}
