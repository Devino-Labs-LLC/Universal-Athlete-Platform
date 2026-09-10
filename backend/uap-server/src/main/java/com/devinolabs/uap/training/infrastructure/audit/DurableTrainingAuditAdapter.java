package com.devinolabs.uap.training.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.training.application.TrainingAuditPort;

@Component
class DurableTrainingAuditAdapter implements TrainingAuditPort {

	private final SecurityAuditWriter auditWriter;

	DurableTrainingAuditAdapter(SecurityAuditWriter auditWriter) {
		this.auditWriter = Objects.requireNonNull(auditWriter);
	}

	@Override
	public void workoutAssigned(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		append("WORKOUT_ASSIGNED", assignmentId, actorAccountId, athleteId, teamId, organizationId);
	}

	@Override
	public void workoutAssignmentModified(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		append("WORKOUT_ASSIGNMENT_MODIFIED", assignmentId, actorAccountId, athleteId, teamId, organizationId);
	}

	@Override
	public void workoutAssignmentDeclined(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		append("WORKOUT_ASSIGNMENT_DECLINED", assignmentId, actorAccountId, athleteId, teamId, organizationId);
	}

	@Override
	public void workoutAssignmentUnable(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		append("WORKOUT_ASSIGNMENT_UNABLE", assignmentId, actorAccountId, athleteId, teamId, organizationId);
	}

	private void append(
			String eventType,
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				Objects.requireNonNull(actorAccountId),
				null,
				Objects.requireNonNull(athleteId),
				Objects.requireNonNull(organizationId),
				Objects.requireNonNull(teamId),
				"TRAINING_ASSIGNMENT",
				Objects.requireNonNull(assignmentId),
				null));
	}

}
