package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;

public record WorkoutAdaptationProposalPage(
		List<WorkoutAdaptationProposal> proposals,
		int page,
		int size,
		long totalElements) {
}
