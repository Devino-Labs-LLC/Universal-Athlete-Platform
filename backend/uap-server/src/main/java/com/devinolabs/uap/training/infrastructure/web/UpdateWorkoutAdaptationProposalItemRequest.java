package com.devinolabs.uap.training.infrastructure.web;

import java.util.UUID;

import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;

record UpdateWorkoutAdaptationProposalItemRequest(
		WorkoutAdaptationDecision decision,
		UUID targetExerciseDefinitionId,
		UUID substitutionRelationshipId,
		String athleteNotes) {
}
