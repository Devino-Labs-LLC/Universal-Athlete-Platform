package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.training.domain.TrainingAssignment;
import com.devinolabs.uap.training.domain.TrainingAssignmentStatus;

@Service
public class AthleteTrainingAssignmentUseCase {

	private static final int LIST_LIMIT = 50;

	private final AthleteContextPort athleteContextPort;
	private final TrainingAssignmentRepository assignments;
	private final TrainingAuditPort auditPort;
	private final Clock clock;

	public AthleteTrainingAssignmentUseCase(
			AthleteContextPort athleteContextPort,
			TrainingAssignmentRepository assignments,
			TrainingAuditPort auditPort,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.assignments = Objects.requireNonNull(assignments);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public List<TrainingAssignment> listMine(UUID accountId) {
		UUID athleteId = athleteContextPort.requireAthlete(accountId).athleteId();
		return assignments.findByAthlete(athleteId, LIST_LIMIT);
	}

	@Transactional
	public TrainingAssignment decline(UUID accountId, UUID assignmentId, String note) {
		return respond(accountId, assignmentId, TrainingAssignmentStatus.DECLINED, note);
	}

	@Transactional
	public TrainingAssignment markUnable(UUID accountId, UUID assignmentId, String note) {
		return respond(accountId, assignmentId, TrainingAssignmentStatus.UNABLE, note);
	}

	private TrainingAssignment respond(
			UUID accountId,
			UUID assignmentId,
			TrainingAssignmentStatus status,
			String note) {
		UUID athleteId = athleteContextPort.requireAthlete(accountId).athleteId();
		TrainingAssignment assignment = assignments.findById(assignmentId)
				.orElseThrow(TrainingAssignmentNotFoundException::new);
		if (!athleteId.equals(assignment.athleteId())) {
			throw new TrainingAssignmentNotFoundException();
		}
		TrainingAssignmentStatus previous = assignment.status();
		try {
			if (status == TrainingAssignmentStatus.DECLINED) {
				assignment.decline(note, clock);
			}
			else {
				assignment.markUnable(note, clock);
			}
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingAssignmentException(ex.getMessage());
		}
		catch (IllegalStateException ex) {
			throw new TrainingAssignmentConflictException("Assignment response is already recorded");
		}
		if (previous == assignment.status() && assignment.isTerminal()) {
			return assignment;
		}
		TrainingAssignment saved = assignments.save(assignment);
		if (status == TrainingAssignmentStatus.DECLINED) {
			auditPort.workoutAssignmentDeclined(
					saved.id(),
					accountId,
					athleteId,
					saved.teamId(),
					saved.organizationId());
		}
		else {
			auditPort.workoutAssignmentUnable(
					saved.id(),
					accountId,
					athleteId,
					saved.teamId(),
					saved.organizationId());
		}
		return saved;
	}

}
