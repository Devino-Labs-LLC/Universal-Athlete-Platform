package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.ActiveWorkoutAdaptationProposalExistsException;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalFilters;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalPage;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalRepository;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalVersionConflictException;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposal;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Repository
class JpaWorkoutAdaptationProposalRepository implements WorkoutAdaptationProposalRepository {

	private static final List<WorkoutAdaptationProposalStatus> ACTIVE_STATUSES = List.of(
			WorkoutAdaptationProposalStatus.DRAFT,
			WorkoutAdaptationProposalStatus.READY,
			WorkoutAdaptationProposalStatus.PARTIALLY_RESOLVED);

	private final WorkoutAdaptationProposalJpaRepository jpaRepository;

	JpaWorkoutAdaptationProposalRepository(WorkoutAdaptationProposalJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutAdaptationProposal save(WorkoutAdaptationProposal proposal) {
		try {
			WorkoutAdaptationProposalJpaEntity entity = jpaRepository
					.findOwnedById(proposal.id().value(), proposal.athleteId().value())
					.map(existing -> WorkoutAdaptationProposalPersistenceMapper.toEntity(proposal, existing))
					.orElseGet(() -> WorkoutAdaptationProposalPersistenceMapper.toEntity(proposal));
			WorkoutAdaptationProposalJpaEntity saved = jpaRepository.save(entity);
			jpaRepository.flush();
			return toDomainWithChildren(saved);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new WorkoutAdaptationProposalVersionConflictException();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutAdaptationProposal> findOwnedById(WorkoutAdaptationProposalId id, AthleteId athleteId) {
		return jpaRepository.findOwnedById(id.value(), athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<WorkoutAdaptationProposal> lockOwnedById(WorkoutAdaptationProposalId id, AthleteId athleteId) {
		return jpaRepository.lockOwnedById(id.value(), athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<WorkoutAdaptationProposal> findActiveByOccurrenceId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findActiveByOccurrenceId(occurrenceId.value(), athleteId.value(), ACTIVE_STATUSES)
				.stream()
				.findFirst()
				.map(this::toDomainWithChildren);
	}

	@Override
	public WorkoutAdaptationProposalPage findByAthlete(
			AthleteId athleteId,
			WorkoutAdaptationProposalFilters filters,
			int page,
			int size) {
		Page<WorkoutAdaptationProposalJpaEntity> found = jpaRepository.findFiltered(
				athleteId.value(),
				filters.occurrenceId() == null ? null : filters.occurrenceId().value(),
				filters.status(),
				PageRequest.of(page, size));
		List<WorkoutAdaptationProposal> proposals = found.getContent().stream()
				.map(summaryEntity -> jpaRepository.findOwnedById(summaryEntity.getId(), athleteId.value())
						.orElseThrow())
				.map(this::toDomainWithChildren)
				.toList();
		return new WorkoutAdaptationProposalPage(proposals, page, size, found.getTotalElements());
	}

	private WorkoutAdaptationProposal toDomainWithChildren(WorkoutAdaptationProposalJpaEntity entity) {
		entity.getRecommendationAdjustments().size();
		for (WorkoutAdaptationRecommendationAdjustmentJpaEntity adjustment : entity.getRecommendationAdjustments()) {
			adjustment.getReasons().size();
			adjustment.getDimensions().size();
		}
		return WorkoutAdaptationProposalPersistenceMapper.toDomain(entity);
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause().getMessage();
		if (message != null && message.contains("uq_adaptation_proposals_active_occurrence")) {
			return new ActiveWorkoutAdaptationProposalExistsException();
		}
		return ex;
	}

}
