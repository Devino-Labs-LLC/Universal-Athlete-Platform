package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.integrations.api.evidence.WearableOpsSummaryView;
import com.devinolabs.uap.integrations.domain.ConnectedDevice;
import com.devinolabs.uap.integrations.domain.DeviceLifecycle;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunStatus;

@Service
public class WearableOpsService {

	private final WearableOpsCounterRepository counters;
	private final SyncRunRepository syncRunRepository;
	private final DeviceRepository deviceRepository;
	private final ConnectionRepository connectionRepository;
	private final AthleteContextPort athleteContextPort;
	private final Clock clock;

	WearableOpsService(
			WearableOpsCounterRepository counters,
			SyncRunRepository syncRunRepository,
			DeviceRepository deviceRepository,
			ConnectionRepository connectionRepository,
			AthleteContextPort athleteContextPort,
			Clock clock) {
		this.counters = Objects.requireNonNull(counters);
		this.syncRunRepository = Objects.requireNonNull(syncRunRepository);
		this.deviceRepository = Objects.requireNonNull(deviceRepository);
		this.connectionRepository = Objects.requireNonNull(connectionRepository);
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void recordSync(HealthProviderKey provider, SyncRun run, int duplicates) {
		boolean success = run.status() == SyncRunStatus.SUCCEEDED || run.status() == SyncRunStatus.PARTIAL;
		counters.increment(
				provider,
				1,
				success ? 1 : 0,
				success ? 0 : 1,
				run.recordsAccepted(),
				run.recordsRejected(),
				duplicates,
				success ? 0 : 1,
				run.errorCode());
	}

	@Transactional
	public void recordIngest(HealthProviderKey provider, int accepted, int rejected, int duplicates) {
		counters.increment(provider, 0, 0, 0, accepted, rejected, duplicates, 0, null);
	}

	@Transactional(readOnly = true)
	public WearableOpsSummaryView summaryForAccount(UUID accountId) {
		AthleteRef athlete = athleteContextPort.requireAthlete(accountId);
		Instant now = Instant.now(clock);
		int attempts = 0;
		int successes = 0;
		int failures = 0;
		int accepted = 0;
		int rejected = 0;
		String lastError = null;
		for (var connection : connectionRepository.findByAccountId(accountId)) {
			List<SyncRun> runs = syncRunRepository.findByConnectionIdOrderByRequestedAtDesc(connection.id());
			for (SyncRun run : runs) {
				attempts++;
				accepted += run.recordsAccepted();
				rejected += run.recordsRejected();
				if (run.status() == SyncRunStatus.FAILED) {
					failures++;
					lastError = run.errorCode();
				}
				else {
					successes++;
				}
			}
		}
		int stale = 0;
		for (ConnectedDevice device : deviceRepository.findByAthleteId(athlete.athleteId())) {
			if (device.effectiveLifecycle(now) == DeviceLifecycle.STALE) {
				stale++;
			}
		}
		return new WearableOpsSummaryView(attempts, successes, failures, accepted, rejected, 0, stale, lastError);
	}
}
