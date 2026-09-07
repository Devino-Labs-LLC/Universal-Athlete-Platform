package com.devinolabs.uap.training.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.training.application.TrainingAuditPort;

@Component
class LoggingTrainingAuditAdapter implements TrainingAuditPort {

	private static final Logger log = LoggerFactory.getLogger(LoggingTrainingAuditAdapter.class);

	@Override
	public void workoutAssigned(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		log.info(
				"training_audit event=WORKOUT_ASSIGNED assignmentId={} actorAccountId={} athleteId={} teamId={} organizationId={}",
				id(assignmentId),
				id(actorAccountId),
				id(athleteId),
				id(teamId),
				id(organizationId));
	}

	@Override
	public void workoutAssignmentModified(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		log.info(
				"training_audit event=WORKOUT_ASSIGNMENT_MODIFIED assignmentId={} actorAccountId={} athleteId={} teamId={} organizationId={}",
				id(assignmentId),
				id(actorAccountId),
				id(athleteId),
				id(teamId),
				id(organizationId));
	}

	@Override
	public void workoutAssignmentDeclined(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		log.info(
				"training_audit event=WORKOUT_ASSIGNMENT_DECLINED assignmentId={} actorAccountId={} athleteId={} teamId={} organizationId={}",
				id(assignmentId),
				id(actorAccountId),
				id(athleteId),
				id(teamId),
				id(organizationId));
	}

	@Override
	public void workoutAssignmentUnable(
			UUID assignmentId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId) {
		log.info(
				"training_audit event=WORKOUT_ASSIGNMENT_UNABLE assignmentId={} actorAccountId={} athleteId={} teamId={} organizationId={}",
				id(assignmentId),
				id(actorAccountId),
				id(athleteId),
				id(teamId),
				id(organizationId));
	}

	private static String id(UUID value) {
		return Objects.toString(value, "");
	}

}
