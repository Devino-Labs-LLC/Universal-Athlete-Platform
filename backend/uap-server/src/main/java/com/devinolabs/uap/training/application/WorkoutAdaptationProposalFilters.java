package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

public record WorkoutAdaptationProposalFilters(
		WorkoutOccurrenceId occurrenceId,
		WorkoutAdaptationProposalStatus status) {

	public static WorkoutAdaptationProposalFilters of(
			WorkoutOccurrenceId occurrenceId,
			WorkoutAdaptationProposalStatus status) {
		return new WorkoutAdaptationProposalFilters(occurrenceId, status);
	}

}
