package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

	List<TeamMembershipJpaEntity> findAllByAccountId(UUID accountId);

	boolean existsByTeamIdAndAccountIdAndStatus(
			UUID teamId,
			UUID accountId,
			OrganizationMembershipStatus status);

	@Query("""
			select count(m) > 0
			from TeamMembershipJpaEntity m, TeamJpaEntity t
			where m.teamId = t.id
			  and t.organizationId = :organizationId
			  and m.athleteId = :athleteId
			  and m.status = com.devinolabs.uap.organization.domain.OrganizationMembershipStatus.ACTIVE
			  and m.role = com.devinolabs.uap.organization.domain.OrganizationMembershipRole.ATHLETE
			""")
	boolean existsActiveAthleteInOrganization(
			@Param("organizationId") UUID organizationId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select count(distinct m.athleteId)
			from TeamMembershipJpaEntity m, TeamJpaEntity t
			where m.teamId = t.id
			  and t.organizationId = :organizationId
			  and m.status = com.devinolabs.uap.organization.domain.OrganizationMembershipStatus.ACTIVE
			  and m.role = com.devinolabs.uap.organization.domain.OrganizationMembershipRole.ATHLETE
			  and m.athleteId is not null
			""")
	long countDistinctActiveAthletes(@Param("organizationId") UUID organizationId);

}
