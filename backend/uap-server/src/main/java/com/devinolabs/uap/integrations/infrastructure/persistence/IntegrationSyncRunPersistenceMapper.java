package com.devinolabs.uap.integrations.infrastructure.persistence;

import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

final class IntegrationSyncRunPersistenceMapper {

	private IntegrationSyncRunPersistenceMapper() {
	}

	static IntegrationSyncRunJpaEntity toEntity(SyncRun syncRun, boolean isNew) {
		return new IntegrationSyncRunJpaEntity(
				syncRun.id().value(),
				syncRun.connectionId().value(),
				syncRun.athleteId(),
				syncRun.status(),
				syncRun.requestedAt(),
				syncRun.startedAt(),
				syncRun.finishedAt(),
				syncRun.errorCode(),
				syncRun.recordsAccepted(),
				syncRun.recordsRejected(),
				syncRun.version(),
				isNew);
	}

	static SyncRun toDomain(IntegrationSyncRunJpaEntity entity) {
		return SyncRun.rehydrate(
				SyncRunId.of(entity.getId()),
				ConnectionId.of(entity.getConnectionId()),
				entity.getAthleteId(),
				entity.getStatus(),
				entity.getRequestedAt(),
				entity.getStartedAt(),
				entity.getFinishedAt(),
				entity.getErrorCode(),
				entity.getRecordsAccepted(),
				entity.getRecordsRejected(),
				entity.getVersion());
	}

}
