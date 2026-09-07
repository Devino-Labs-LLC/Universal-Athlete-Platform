package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.TrainingAssignment;

@Service
public class UpdateCoachTrainingAssignmentUseCase {

	private final CoachTrainingAuthorization authorization;
	private final TrainingAssignmentRepository assignments;
	private final TrainingAuditPort auditPort;
	private final Clock clock;

	public UpdateCoachTrainingAssignmentUseCase(
			CoachTrainingAuthorization authorization,
			TrainingAssignmentRepository assignments,
			TrainingAuditPort auditPort,
			Clock clock) {
		this.authorization = Objects.requireNonNull(authorization);
		this.assignments = Objects.requireNonNull(assignments);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public TrainingAssignment execute(
			UUID accountId,
			UUID teamId,
			UUID athleteId,
			UUID assignmentId,
			long expectedVersion,
			String title,
			String description,
			LocalDate scheduledDate) {
		authorization.requireCollaborationWrite(accountId, teamId, athleteId);
		TrainingAssignment assignment = assignments.findById(assignmentId)
				.orElseThrow(TrainingAssignmentNotFoundException::new);
		if (!teamId.equals(assignment.teamId()) || !athleteId.equals(assignment.athleteId())) {
			throw new TrainingAssignmentNotFoundException();
		}
		if (assignment.version() != expectedVersion) {
			throw new TrainingAssignmentVersionConflictException();
		}
		try {
			assignment.updateContent(expectedVersion, title, description, scheduledDate, clock);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingAssignmentException(ex.getMessage());
		}
		catch (IllegalStateException ex) {
			if (assignment.isTerminal()) {
				throw new TrainingAssignmentConflictException("Assignment is no longer editable");
			}
			throw new TrainingAssignmentVersionConflictException();
		}
		try {
			TrainingAssignment saved = assignments.save(assignment);
			auditPort.workoutAssignmentModified(
					saved.id(),
					accountId,
					athleteId,
					teamId,
					saved.organizationId());
			return saved;
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new TrainingAssignmentVersionConflictException();
		}
	}

}
