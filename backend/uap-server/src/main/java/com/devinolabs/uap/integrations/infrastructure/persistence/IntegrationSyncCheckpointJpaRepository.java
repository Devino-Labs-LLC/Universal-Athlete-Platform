package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface IntegrationSyncCheckpointJpaRepository extends JpaRepository<IntegrationSyncCheckpointJpaEntity, UUID> {
}
