package com.devinolabs.uap.integrations.api.evidence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface FusedSensorPort {

	List<FusedEvidenceView> listFused(
			UUID athleteId,
			Instant fromInclusive,
			Instant toInclusive,
			String signalFamily);
}
