package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationMembershipStatus;

interface TeamMembershipJpaRepository extends JpaRepository<TeamMembershipJpaEntity, UUID> {

	Optional<TeamMembershipJpaEntity> findByTeamIdAndAccountIdAndStatus(
			UUID teamId,
			UUID accountId,
			OrganizationMembershipStatus status);

	Optional<TeamMembershipJpaEntity> findByTeamIdAndAthleteIdAndStatusAndRole(
			UUID teamId,
			UUID athleteId,
			OrganizationMembershipStatus status,
			OrganizationMembershipRole role);

	List<TeamMembershipJpaEntity> findAllByTeamId(UUID teamId);

	List<TeamMembershipJpaEntity> findAllByAccountIdAndStatus(UUID accountId, OrganizationMembershipStatus status);

	boolean existsByTeamIdAndAccountIdAndStatus(
			UUID teamId,
			UUID accountId,
			OrganizationMembershipStatus status);

}
