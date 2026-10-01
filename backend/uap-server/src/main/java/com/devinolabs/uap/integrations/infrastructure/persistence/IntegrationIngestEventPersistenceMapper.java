package com.devinolabs.uap.integrations.infrastructure.persistence;

import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.IngestEvent;
import com.devinolabs.uap.integrations.domain.IngestEventId;
import com.devinolabs.uap.integrations.domain.SyncRunId;

final class IntegrationIngestEventPersistenceMapper {

	private IntegrationIngestEventPersistenceMapper() {
	}

	static IntegrationIngestEventJpaEntity toEntity(IngestEvent event, boolean isNew) {
		return new IntegrationIngestEventJpaEntity(
				event.id().value(),
				event.provider(),
				event.providerEventId(),
				event.eventType(),
				event.connectionId() == null ? null : event.connectionId().value(),
				event.athleteId(),
				event.receivedAt(),
				event.processedAt(),
				event.processingStatus(),
				event.errorCode(),
				event.syncRunId() == null ? null : event.syncRunId().value(),
				isNew);
	}

	static IngestEvent toDomain(IntegrationIngestEventJpaEntity entity) {
		return IngestEvent.rehydrate(
				IngestEventId.of(entity.getId()),
				entity.getProvider(),
				entity.getProviderEventId(),
				entity.getEventType(),
				entity.getConnectionId() == null ? null : ConnectionId.of(entity.getConnectionId()),
				entity.getAthleteId(),
				entity.getReceivedAt(),
				entity.getProcessedAt(),
				entity.getProcessingStatus(),
				entity.getErrorCode(),
				entity.getSyncRunId() == null ? null : SyncRunId.of(entity.getSyncRunId()));
	}
}
