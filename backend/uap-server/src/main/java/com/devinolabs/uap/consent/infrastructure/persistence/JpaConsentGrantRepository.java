package com.devinolabs.uap.consent.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.consent.application.ConsentGrantRepository;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentGrantStatus;

@Repository
class JpaConsentGrantRepository implements ConsentGrantRepository {

	private final ConsentGrantJpaRepository jpaRepository;

	JpaConsentGrantRepository(ConsentGrantJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ConsentGrant save(ConsentGrant grant) {
		Optional<ConsentGrantJpaEntity> existing = jpaRepository.findById(grant.id().value());
		ConsentGrantJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(ConsentGrantPersistenceMapper.toEntity(grant, true));
		}
		else {
			ConsentGrantJpaEntity entity = existing.get();
			entity.applyDomainState(grant.status(), grant.revokedAt(), grant.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return ConsentGrantPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<ConsentGrant> findById(ConsentGrantId id) {
		return jpaRepository.findById(id.value()).map(ConsentGrantPersistenceMapper::toDomain);
	}

	@Override
	public List<ConsentGrant> findActiveByAthleteIdAndTeamId(UUID athleteId, UUID teamId) {
		return jpaRepository.findAllByAthleteIdAndTeamIdAndStatus(athleteId, teamId, ConsentGrantStatus.ACTIVE)
				.stream()
				.map(ConsentGrantPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<ConsentGrant> findActiveByTeamMembershipId(UUID teamMembershipId) {
		return jpaRepository.findByTeamMembershipIdAndStatus(teamMembershipId, ConsentGrantStatus.ACTIVE)
				.map(ConsentGrantPersistenceMapper::toDomain);
	}

	@Override
	public List<ConsentGrant> findAllByAthleteId(UUID athleteId) {
		return jpaRepository.findAllByAthleteIdOrderByCreatedAtDesc(athleteId).stream()
				.map(ConsentGrantPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<ConsentGrant> findAllByAthleteIdAndStatus(UUID athleteId, ConsentGrantStatus status) {
		return jpaRepository.findAllByAthleteIdAndStatus(athleteId, status).stream()
				.map(ConsentGrantPersistenceMapper::toDomain)
				.toList();
	}

}
