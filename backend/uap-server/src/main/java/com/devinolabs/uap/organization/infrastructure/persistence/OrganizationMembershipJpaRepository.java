package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

interface OrganizationMembershipJpaRepository extends JpaRepository<OrganizationMembershipJpaEntity, UUID> {

	Optional<OrganizationMembershipJpaEntity> findByOrganizationIdAndAccountIdAndStatus(
			UUID organizationId,
			UUID accountId,
			OrganizationMembershipStatus status);

	Optional<OrganizationMembershipJpaEntity> findByOrganizationIdAndAccountIdAndStatusAndRole(
			UUID organizationId,
			UUID accountId,
			OrganizationMembershipStatus status,
			OrganizationMembershipRole role);

	List<OrganizationMembershipJpaEntity> findAllByAccountIdAndStatus(
			UUID accountId,
			OrganizationMembershipStatus status);

	boolean existsByOrganizationIdAndAccountIdAndStatus(
			UUID organizationId,
			UUID accountId,
			OrganizationMembershipStatus status);

	boolean existsByOrganizationIdAndAccountIdAndStatusAndRole(
			UUID organizationId,
			UUID accountId,
			OrganizationMembershipStatus status,
			OrganizationMembershipRole role);

}
