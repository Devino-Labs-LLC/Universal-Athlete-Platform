package com.devinolabs.uap.training.application;

import java.util.UUID;

public interface TrainingAuditPort {

	void workoutAssigned(UUID assignmentId, UUID actorAccountId, UUID athleteId, UUID teamId, UUID organizationId);

	void workoutAssignmentModified(UUID assignmentId, UUID actorAccountId, UUID athleteId, UUID teamId, UUID organizationId);

	void workoutAssignmentDeclined(UUID assignmentId, UUID actorAccountId, UUID athleteId, UUID teamId, UUID organizationId);

	void workoutAssignmentUnable(UUID assignmentId, UUID actorAccountId, UUID athleteId, UUID teamId, UUID organizationId);

}
