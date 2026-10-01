package com.devinolabs.uap.integrations.infrastructure.persistence;

import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.EvidenceId;
import com.devinolabs.uap.integrations.domain.SyncRunId;

final class IntegrationEvidencePersistenceMapper {

	private IntegrationEvidencePersistenceMapper() {
	}

	static IntegrationEvidenceJpaEntity toEntity(ConnectedEvidence evidence, boolean isNew) {
		return new IntegrationEvidenceJpaEntity(
				evidence.id().value(),
				evidence.athleteId(),
				evidence.connectionId().value(),
				evidence.provider(),
				evidence.signalFamily(),
				evidence.signalType(),
				evidence.externalRecordId(),
				evidence.valueNumeric(),
				evidence.valueText(),
				evidence.unitCode(),
				evidence.periodStart(),
				evidence.periodEnd(),
				evidence.observedAt(),
				evidence.providerUpdatedAt(),
				evidence.ingestedAt(),
				evidence.syncRunId() == null ? null : evidence.syncRunId().value(),
				evidence.provenanceClass(),
				evidence.sourceDeviceOrApp(),
				evidence.qualityCode(),
				evidence.status(),
				evidence.createdAt(),
				evidence.updatedAt(),
				evidence.version(),
				isNew);
	}

	static ConnectedEvidence toDomain(IntegrationEvidenceJpaEntity entity) {
		return ConnectedEvidence.rehydrate(
				EvidenceId.of(entity.getId()),
				entity.getAthleteId(),
				ConnectionId.of(entity.getConnectionId()),
				entity.getProvider(),
				entity.getSignalFamily(),
				entity.getSignalType(),
				entity.getExternalRecordId(),
				entity.getValueNumeric(),
				entity.getValueText(),
				entity.getUnitCode(),
				entity.getPeriodStart(),
				entity.getPeriodEnd(),
				entity.getObservedAt(),
				entity.getProviderUpdatedAt(),
				entity.getIngestedAt(),
				entity.getSyncRunId() == null ? null : SyncRunId.of(entity.getSyncRunId()),
				entity.getProvenanceClass(),
				entity.getSourceDeviceOrApp(),
				entity.getQualityCode(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}
}
