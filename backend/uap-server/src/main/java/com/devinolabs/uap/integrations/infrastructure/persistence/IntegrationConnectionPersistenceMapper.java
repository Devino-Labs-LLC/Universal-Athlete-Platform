package com.devinolabs.uap.integrations.infrastructure.persistence;

import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;

final class IntegrationConnectionPersistenceMapper {

	private IntegrationConnectionPersistenceMapper() {
	}

	static IntegrationConnectionJpaEntity toEntity(Connection connection, boolean isNew) {
		return new IntegrationConnectionJpaEntity(
				connection.id().value(),
				connection.athleteId(),
				connection.accountId(),
				connection.provider(),
				connection.status(),
				connection.processConsentGranted(),
				connection.processConsentGrantedAt(),
				connection.scopesJson(),
				connection.connectedAt(),
				connection.disconnectedAt(),
				connection.lastSuccessfulSyncAt(),
				connection.lastAttemptedSyncAt(),
				connection.providerUserRef(),
				connection.createdAt(),
				connection.updatedAt(),
				connection.version(),
				isNew);
	}

	static Connection toDomain(IntegrationConnectionJpaEntity entity) {
		return Connection.rehydrate(
				ConnectionId.of(entity.getId()),
				entity.getAthleteId(),
				entity.getAccountId(),
				entity.getProvider(),
				entity.getLifecycleState(),
				entity.isProcessConsentGranted(),
				entity.getProcessConsentGrantedAt(),
				entity.getScopesJson(),
				entity.getConnectedAt(),
				entity.getDisconnectedAt(),
				entity.getLastSuccessfulSyncAt(),
				entity.getLastAttemptedSyncAt(),
				entity.getProviderUserRef(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
