package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

@Repository
class JpaTeamMembershipRepository implements TeamMembershipRepository {

	private final TeamMembershipJpaRepository jpaRepository;

	JpaTeamMembershipRepository(TeamMembershipJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public TeamMembership save(TeamMembership membership) {
		Optional<TeamMembershipJpaEntity> existing = jpaRepository.findById(membership.id().value());
		TeamMembershipJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(TeamMembershipPersistenceMapper.toEntity(membership, true));
		}
		else {
			TeamMembershipJpaEntity entity = existing.get();
			entity.applyDomainState(membership.status(), membership.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return TeamMembershipPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<TeamMembership> findById(TeamMembershipId id) {
		return jpaRepository.findById(id.value()).map(TeamMembershipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<TeamMembership> findActiveByTeamIdAndAccountId(TeamId teamId, AccountId accountId) {
		return jpaRepository.findByTeamIdAndAccountIdAndStatus(
						teamId.value(),
						accountId.value(),
						OrganizationMembershipStatus.ACTIVE)
				.map(TeamMembershipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<TeamMembership> findActiveByTeamIdAndAthleteId(TeamId teamId, java.util.UUID athleteId) {
		return jpaRepository.findByTeamIdAndAthleteIdAndStatusAndRole(
						teamId.value(),
						athleteId,
						OrganizationMembershipStatus.ACTIVE,
						OrganizationMembershipRole.ATHLETE)
				.map(TeamMembershipPersistenceMapper::toDomain);
	}

	@Override
	public List<TeamMembership> findAllByTeamId(TeamId teamId) {
		return jpaRepository.findAllByTeamId(teamId.value()).stream()
				.map(TeamMembershipPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<TeamMembership> findAllActiveByAccountId(AccountId accountId) {
		return jpaRepository.findAllByAccountIdAndStatus(accountId.value(), OrganizationMembershipStatus.ACTIVE)
				.stream()
				.map(TeamMembershipPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsActiveMembership(AccountId accountId, TeamId teamId) {
		return jpaRepository.existsByTeamIdAndAccountIdAndStatus(
				teamId.value(),
				accountId.value(),
				OrganizationMembershipStatus.ACTIVE);
	}

}
