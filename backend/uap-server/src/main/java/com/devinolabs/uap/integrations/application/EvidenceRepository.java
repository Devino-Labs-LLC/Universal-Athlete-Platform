package com.devinolabs.uap.integrations.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SignalFamily;

public interface EvidenceRepository {

	ConnectedEvidence save(ConnectedEvidence evidence);

	Optional<ConnectedEvidence> findByProviderAndAthleteIdAndExternalRecordId(
			HealthProviderKey provider,
			UUID athleteId,
			String externalRecordId);

	List<ConnectedEvidence> findByAthleteIdAndObservedAtBetween(
			UUID athleteId,
			Instant fromInclusive,
			Instant toInclusive,
			SignalFamily familyOrNull);

	List<ConnectedEvidence> findByConnectionIdAndObservedAtBetween(
			ConnectionId connectionId,
			Instant fromInclusive,
			Instant toInclusive,
			SignalFamily familyOrNull);

	long countByConnectionId(ConnectionId connectionId);
}
