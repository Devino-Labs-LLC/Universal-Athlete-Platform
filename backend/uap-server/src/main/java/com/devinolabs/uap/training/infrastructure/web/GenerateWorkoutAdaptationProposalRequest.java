package com.devinolabs.uap.training.infrastructure.web;

record GenerateWorkoutAdaptationProposalRequest(
		Integer suggestionLimit,
		Boolean includeAlternatives,
		Integer expirationMinutes) {
}
