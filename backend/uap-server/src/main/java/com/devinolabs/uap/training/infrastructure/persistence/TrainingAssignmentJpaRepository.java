package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingAssignmentJpaRepository extends JpaRepository<TrainingAssignmentJpaEntity, UUID> {

	Optional<TrainingAssignmentJpaEntity> findByAssignedByAccountIdAndTeamIdAndAthleteIdAndIdempotencyKey(
			UUID assignedByAccountId,
			UUID teamId,
			UUID athleteId,
			String idempotencyKey);

	List<TrainingAssignmentJpaEntity> findByTeamIdAndAthleteIdOrderByScheduledDateDescIdAsc(
			UUID teamId,
			UUID athleteId,
			Pageable pageable);

	List<TrainingAssignmentJpaEntity> findByAthleteIdOrderByScheduledDateDescIdAsc(UUID athleteId, Pageable pageable);

}
