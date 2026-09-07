package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.TrainingAssignmentRepository;
import com.devinolabs.uap.training.domain.TrainingAssignment;

@Repository
class JpaTrainingAssignmentRepository implements TrainingAssignmentRepository {

	private final TrainingAssignmentJpaRepository jpaRepository;

	JpaTrainingAssignmentRepository(TrainingAssignmentJpaRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public TrainingAssignment save(TrainingAssignment assignment) {
		TrainingAssignmentJpaEntity entity = jpaRepository.findById(assignment.id())
				.orElseGet(TrainingAssignmentJpaEntity::new);
		boolean isNew = entity.isNew();
		entity.apply(
				assignment.id(),
				assignment.athleteId(),
				assignment.teamId(),
				assignment.organizationId(),
				assignment.athleteMembershipId(),
				assignment.assignedByAccountId(),
				assignment.assignedByRole(),
				assignment.title(),
				assignment.description(),
				assignment.scheduledDate(),
				assignment.status(),
				assignment.athleteResponseNote(),
				assignment.respondedAt(),
				assignment.idempotencyKey(),
				assignment.createdAt(),
				assignment.updatedAt(),
				assignment.version(),
				isNew);
		TrainingAssignmentJpaEntity saved = jpaRepository.saveAndFlush(entity);
		assignment.assignPersistedVersion(saved.getVersion());
		return assignment;
	}

	@Override
	public Optional<TrainingAssignment> findById(UUID id) {
		return jpaRepository.findById(id).map(JpaTrainingAssignmentRepository::toDomain);
	}

	@Override
	public Optional<TrainingAssignment> findByActorIdempotency(
			UUID assignedByAccountId,
			UUID teamId,
			UUID athleteId,
			String idempotencyKey) {
		return jpaRepository
				.findByAssignedByAccountIdAndTeamIdAndAthleteIdAndIdempotencyKey(
						assignedByAccountId,
						teamId,
						athleteId,
						idempotencyKey)
				.map(JpaTrainingAssignmentRepository::toDomain);
	}

	@Override
	public List<TrainingAssignment> findByTeamAndAthlete(UUID teamId, UUID athleteId, int limit) {
		return jpaRepository
				.findByTeamIdAndAthleteIdOrderByScheduledDateDescIdAsc(teamId, athleteId, PageRequest.of(0, limit))
				.stream()
				.map(JpaTrainingAssignmentRepository::toDomain)
				.toList();
	}

	@Override
	public List<TrainingAssignment> findByAthlete(UUID athleteId, int limit) {
		return jpaRepository
				.findByAthleteIdOrderByScheduledDateDescIdAsc(athleteId, PageRequest.of(0, limit))
				.stream()
				.map(JpaTrainingAssignmentRepository::toDomain)
				.toList();
	}

	private static TrainingAssignment toDomain(TrainingAssignmentJpaEntity entity) {
		return TrainingAssignment.rehydrate(
				entity.getId(),
				entity.getAthleteId(),
				entity.getTeamId(),
				entity.getOrganizationId(),
				entity.getAthleteMembershipId(),
				entity.getAssignedByAccountId(),
				entity.getAssignedByRole(),
				entity.getTitle(),
				entity.getDescription(),
				entity.getScheduledDate(),
				entity.getStatus(),
				entity.getAthleteResponseNote(),
				entity.getRespondedAt(),
				entity.getIdempotencyKey(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
