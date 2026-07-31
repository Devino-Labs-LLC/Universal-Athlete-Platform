package com.devinolabs.uap.training.application;

public record TrainingLoadRebuildResult(
		int completedOccurrencesScanned,
		int summariesCreated,
		int summariesUpdated,
		int summariesUnchanged) {
}
