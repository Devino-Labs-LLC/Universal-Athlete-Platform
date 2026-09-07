package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.TrainingAssignment;

@Service
public class AssignCoachTrainingUseCase {

	private static final int LIST_LIMIT = 50;

	private final CoachTrainingAuthorization authorization;
	private final TrainingAssignmentRepository assignments;
	private final TrainingAuditPort auditPort;
	private final Clock clock;

	public AssignCoachTrainingUseCase(
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
			String title,
			String description,
			LocalDate scheduledDate,
			String idempotencyKey) {
		CoachTrainingAuthorization.AuthorizedCoachAssignment access =
				authorization.requireCollaborationWrite(accountId, teamId, athleteId);
		String key = requireKey(idempotencyKey);
		LocalDate date = Objects.requireNonNull(scheduledDate, "scheduledDate must not be null");

		Optional<TrainingAssignment> existing = assignments.findByActorIdempotency(accountId, teamId, athleteId, key);
		if (existing.isPresent()) {
			return replayOrConflict(existing.get(), title, description, date);
		}

		TrainingAssignment created;
		try {
			created = TrainingAssignment.assign(
					UUID.randomUUID(),
					athleteId,
					teamId,
					access.lifecycle().organizationId(),
					access.athlete().membershipId(),
					accountId,
					access.coach().role(),
					title,
					description,
					date,
					key,
					clock);
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingAssignmentException(ex.getMessage());
		}

		try {
			TrainingAssignment saved = assignments.save(created);
			auditPort.workoutAssigned(
					saved.id(),
					accountId,
					athleteId,
					teamId,
					saved.organizationId());
			return saved;
		}
		catch (DataIntegrityViolationException ex) {
			return assignments.findByActorIdempotency(accountId, teamId, athleteId, key)
					.map(found -> replayOrConflict(found, title, description, date))
					.orElseThrow(() -> ex);
		}
	}

	@Transactional(readOnly = true)
	public java.util.List<TrainingAssignment> list(UUID accountId, UUID teamId, UUID athleteId) {
		authorization.requireCollaborationWrite(accountId, teamId, athleteId);
		return assignments.findByTeamAndAthlete(teamId, athleteId, LIST_LIMIT);
	}

	@Transactional(readOnly = true)
	public TrainingAssignment get(UUID accountId, UUID teamId, UUID athleteId, UUID assignmentId) {
		authorization.requireCollaborationWrite(accountId, teamId, athleteId);
		TrainingAssignment assignment = assignments.findById(assignmentId)
				.orElseThrow(TrainingAssignmentNotFoundException::new);
		if (!teamId.equals(assignment.teamId()) || !athleteId.equals(assignment.athleteId())) {
			throw new TrainingAssignmentNotFoundException();
		}
		return assignment;
	}

	private static TrainingAssignment replayOrConflict(
			TrainingAssignment existing,
			String title,
			String description,
			LocalDate scheduledDate) {
		try {
			if (existing.sameIntent(title, description, scheduledDate)) {
				return existing;
			}
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingAssignmentException(ex.getMessage());
		}
		throw new TrainingAssignmentConflictException("Idempotency key was already used for a different assignment");
	}

	private static String requireKey(String idempotencyKey) {
		if (idempotencyKey == null || idempotencyKey.isBlank()) {
			throw new InvalidTrainingAssignmentException("idempotencyKey must not be blank");
		}
		return idempotencyKey.trim();
	}

}
