package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface TeamJpaRepository extends JpaRepository<TeamJpaEntity, UUID> {

	List<TeamJpaEntity> findAllByOrganizationId(UUID organizationId);

	boolean existsByOrganizationIdAndName(UUID organizationId, String name);

}
