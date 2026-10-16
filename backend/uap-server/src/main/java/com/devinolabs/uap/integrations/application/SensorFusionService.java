package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidenceView;
import com.devinolabs.uap.integrations.api.evidence.FusedEvidenceView;
import com.devinolabs.uap.integrations.api.evidence.FusedSensorPort;
import com.devinolabs.uap.integrations.domain.ConnectedDevice;
import com.devinolabs.uap.integrations.domain.ConnectedEvidence;
import com.devinolabs.uap.integrations.domain.DeviceCapability;
import com.devinolabs.uap.integrations.domain.DeviceId;
import com.devinolabs.uap.integrations.domain.EvidenceQuality;
import com.devinolabs.uap.integrations.domain.EvidenceQualityPolicy;
import com.devinolabs.uap.integrations.domain.SensorFusionPolicy;
import com.devinolabs.uap.integrations.domain.SensorFusionPolicy.FusionCandidate;
import com.devinolabs.uap.integrations.domain.SensorFusionPolicy.FusionWinner;
import com.devinolabs.uap.integrations.domain.SignalFamily;

@Service
public class SensorFusionService implements FusedSensorPort {

	private final EvidenceRepository evidenceRepository;
	private final DeviceRepository deviceRepository;
	private final IntegrationEvidenceService evidenceService;
	private final Clock clock;

	SensorFusionService(
			EvidenceRepository evidenceRepository,
			DeviceRepository deviceRepository,
			IntegrationEvidenceService evidenceService,
			Clock clock) {
		this.evidenceRepository = Objects.requireNonNull(evidenceRepository);
		this.deviceRepository = Objects.requireNonNull(deviceRepository);
		this.evidenceService = Objects.requireNonNull(evidenceService);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	@Transactional(readOnly = true)
	public List<FusedEvidenceView> listFused(
			UUID athleteId,
			Instant fromInclusive,
			Instant toInclusive,
			String signalFamily) {
		List<ConnectedEvidenceView> rows = evidenceService.listEvidence(
				athleteId, fromInclusive, toInclusive, signalFamily);
		Map<UUID, ConnectedEvidenceView> byId = new HashMap<>();
		for (ConnectedEvidenceView row : rows) {
			byId.put(row.evidenceId(), row);
		}
		Instant now = Instant.now(clock);
		Map<UUID, Set<DeviceCapability>> caps = new HashMap<>();
		List<FusionCandidate> candidates = new ArrayList<>();
		for (ConnectedEvidenceView row : rows) {
			if (!"ACTIVE".equals(row.status())) {
				continue;
			}
			Set<DeviceCapability> capabilities = Set.of();
			if (row.deviceId() != null) {
				capabilities = caps.computeIfAbsent(row.deviceId(), id -> deviceRepository
						.findById(DeviceId.of(id))
						.map(ConnectedDevice::capabilities)
						.orElse(Set.of()));
			}
			SignalFamily family = SignalFamily.valueOf(row.signalFamily());
			EvidenceQuality quality = EvidenceQualityPolicy.derive(
					row.provenanceClass() == null
							? null
							: com.devinolabs.uap.integrations.domain.ProvenanceClass.valueOf(row.provenanceClass()),
					EvidenceQuality.parseOrUnknown(row.qualityCode()),
					family,
					row.observedAt(),
					now,
					capabilities);
			candidates.add(new FusionCandidate(
					row.evidenceId(),
					row.deviceId(),
					family,
					row.signalType(),
					row.observedAt(),
					row.provenanceClass() == null
							? null
							: com.devinolabs.uap.integrations.domain.ProvenanceClass.valueOf(row.provenanceClass()),
					quality,
					capabilities));
		}
		List<FusedEvidenceView> fused = new ArrayList<>();
		for (FusionWinner winner : SensorFusionPolicy.fuse(candidates, now)) {
			ConnectedEvidenceView source = byId.get(winner.winner().evidenceId());
			if (source == null) {
				continue;
			}
			fused.add(new FusedEvidenceView(
					source.evidenceId(),
					source.deviceId(),
					source.provider(),
					source.signalFamily(),
					source.signalType(),
					source.valueNumeric(),
					source.unitCode(),
					source.observedAt(),
					source.provenanceClass(),
					winner.winner().quality().name(),
					winner.score(),
					winner.consideredEvidenceIds()));
		}
		return List.copyOf(fused);
	}

	public List<ConnectedEvidence> originalsUnchanged(UUID athleteId, Instant from, Instant to) {
		return evidenceRepository.findByAthleteIdAndObservedAtBetween(athleteId, from, to, null);
	}
}
