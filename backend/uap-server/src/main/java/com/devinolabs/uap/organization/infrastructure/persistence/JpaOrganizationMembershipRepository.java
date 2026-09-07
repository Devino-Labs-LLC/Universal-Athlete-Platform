package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

@Repository
class JpaOrganizationMembershipRepository implements OrganizationMembershipRepository {

	private final OrganizationMembershipJpaRepository jpaRepository;

	JpaOrganizationMembershipRepository(OrganizationMembershipJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public OrganizationMembership save(OrganizationMembership membership) {
		Optional<OrganizationMembershipJpaEntity> existing = jpaRepository.findById(membership.id().value());
		OrganizationMembershipJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(OrganizationMembershipPersistenceMapper.toEntity(membership, true));
		}
		else {
			OrganizationMembershipJpaEntity entity = existing.get();
			entity.applyDomainState(membership.status(), membership.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return OrganizationMembershipPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<OrganizationMembership> findById(OrganizationMembershipId id) {
		return jpaRepository.findById(id.value()).map(OrganizationMembershipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<OrganizationMembership> findActiveByOrganizationIdAndAccountId(
			OrganizationId organizationId,
			AccountId accountId) {
		return jpaRepository.findByOrganizationIdAndAccountIdAndStatus(
						organizationId.value(),
						accountId.value(),
						OrganizationMembershipStatus.ACTIVE)
				.map(OrganizationMembershipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<OrganizationMembership> findActiveOwner(OrganizationId organizationId, AccountId accountId) {
		return jpaRepository.findByOrganizationIdAndAccountIdAndStatusAndRole(
						organizationId.value(),
						accountId.value(),
						OrganizationMembershipStatus.ACTIVE,
						OrganizationMembershipRole.ORG_OWNER)
				.map(OrganizationMembershipPersistenceMapper::toDomain);
	}

	@Override
	public List<OrganizationMembership> findAllActiveByAccountId(AccountId accountId) {
		return jpaRepository.findAllByAccountIdAndStatus(accountId.value(), OrganizationMembershipStatus.ACTIVE)
				.stream()
				.map(OrganizationMembershipPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<OrganizationMembership> findAllByOrganizationId(OrganizationId organizationId) {
		return jpaRepository.findAllByOrganizationId(organizationId.value()).stream()
				.map(OrganizationMembershipPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public long countActiveOwners(OrganizationId organizationId) {
		return jpaRepository.countByOrganizationIdAndStatusAndRole(
				organizationId.value(),
				OrganizationMembershipStatus.ACTIVE,
				OrganizationMembershipRole.ORG_OWNER);
	}

	@Override
	public boolean existsActiveMembership(AccountId accountId, OrganizationId organizationId) {
		return jpaRepository.existsByOrganizationIdAndAccountIdAndStatus(
				organizationId.value(),
				accountId.value(),
				OrganizationMembershipStatus.ACTIVE);
	}

	@Override
	public boolean existsActiveOwner(AccountId accountId, OrganizationId organizationId) {
		return jpaRepository.existsByOrganizationIdAndAccountIdAndStatusAndRole(
				organizationId.value(),
				accountId.value(),
				OrganizationMembershipStatus.ACTIVE,
				OrganizationMembershipRole.ORG_OWNER);
	}

}
