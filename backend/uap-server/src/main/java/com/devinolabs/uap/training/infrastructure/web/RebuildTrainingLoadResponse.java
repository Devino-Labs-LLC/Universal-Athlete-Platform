package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.TrainingLoadRebuildResult;

record RebuildTrainingLoadResponse(
		int completedOccurrencesScanned,
		int summariesCreated,
		int summariesUpdated,
		int summariesUnchanged) {

	static RebuildTrainingLoadResponse from(TrainingLoadRebuildResult result) {
		return new RebuildTrainingLoadResponse(
				result.completedOccurrencesScanned(),
				result.summariesCreated(),
				result.summariesUpdated(),
				result.summariesUnchanged());
	}

}
