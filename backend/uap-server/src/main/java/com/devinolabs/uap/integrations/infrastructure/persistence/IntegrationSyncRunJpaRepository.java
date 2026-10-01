package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface IntegrationSyncRunJpaRepository extends JpaRepository<IntegrationSyncRunJpaEntity, UUID> {

	List<IntegrationSyncRunJpaEntity> findAllByConnectionIdOrderByRequestedAtDesc(UUID connectionId);

}
