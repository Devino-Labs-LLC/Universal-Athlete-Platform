package com.devinolabs.uap.organization.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.organization.application.InvitationRepository;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.TeamId;

@Repository
class JpaInvitationRepository implements InvitationRepository {

	private final InvitationJpaRepository jpaRepository;

	JpaInvitationRepository(InvitationJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Invitation save(Invitation invitation) {
		Optional<InvitationJpaEntity> existing = jpaRepository.findById(invitation.id().value());
		InvitationJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(InvitationPersistenceMapper.toEntity(invitation, true));
		}
		else {
			InvitationJpaEntity entity = existing.get();
			entity.applyDomainState(invitation.status(), invitation.acceptedMembershipId(), invitation.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return InvitationPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Invitation> findById(InvitationId id) {
		return jpaRepository.findById(id.value()).map(InvitationPersistenceMapper::toDomain);
	}

	@Override
	public Optional<Invitation> findByTokenHash(String tokenHash) {
		return jpaRepository.findByTokenHash(tokenHash).map(InvitationPersistenceMapper::toDomain);
	}

	@Override
	public Optional<Invitation> findByTokenHashForUpdate(String tokenHash) {
		return jpaRepository.findByTokenHashForUpdate(tokenHash).map(InvitationPersistenceMapper::toDomain);
	}

	@Override
	public Optional<Invitation> findByIdForUpdate(InvitationId id) {
		return jpaRepository.findByIdForUpdate(id.value()).map(InvitationPersistenceMapper::toDomain);
	}

	@Override
	public List<Invitation> findAllByTeamId(TeamId teamId) {
		return jpaRepository.findAllByTeamId(teamId.value()).stream()
				.map(InvitationPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<Invitation> findAllByOrganizationIdAndTeamIdIsNull(OrganizationId organizationId) {
		return jpaRepository.findAllByOrganizationIdAndTeamIdIsNull(organizationId.value()).stream()
				.map(InvitationPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<Invitation> findPendingByInvitedEmail(String invitedEmail) {
		return jpaRepository.findAllByInvitedEmailAndStatus(invitedEmail, InvitationStatus.PENDING).stream()
				.map(InvitationPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public int acceptIfPending(InvitationId id, UUID acceptedMembershipId, Instant updatedAt) {
		return jpaRepository.acceptIfPending(id.value(), acceptedMembershipId, updatedAt);
	}

}
