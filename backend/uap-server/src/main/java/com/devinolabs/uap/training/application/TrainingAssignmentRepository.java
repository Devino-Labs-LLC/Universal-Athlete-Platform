package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TrainingAssignment;

public interface TrainingAssignmentRepository {

	TrainingAssignment save(TrainingAssignment assignment);

	Optional<TrainingAssignment> findById(UUID id);

	Optional<TrainingAssignment> findByActorIdempotency(
			UUID assignedByAccountId,
			UUID teamId,
			UUID athleteId,
			String idempotencyKey);

	List<TrainingAssignment> findByTeamAndAthlete(UUID teamId, UUID athleteId, int limit);

	List<TrainingAssignment> findByAthlete(UUID athleteId, int limit);

}
