package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.integrations.application.WearableOpsCounterRepository;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Repository
class JpaWearableOpsCounterRepository implements WearableOpsCounterRepository {

	private final IntegrationOpsCounterJpaRepository jpaRepository;
	private final Clock clock;

	JpaWearableOpsCounterRepository(IntegrationOpsCounterJpaRepository jpaRepository, Clock clock) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	@Transactional
	public void increment(
			HealthProviderKey provider,
			int syncAttempts,
			int syncSuccesses,
			int syncFailures,
			int evidenceAccepted,
			int evidenceRejected,
			int duplicates,
			int adapterErrors,
			String errorCategory) {
		Instant window = Instant.now(clock).truncatedTo(ChronoUnit.HOURS);
		IntegrationOpsCounterJpaEntity row = jpaRepository
				.findByProviderAndWindowStart(provider, window)
				.orElseGet(() -> new IntegrationOpsCounterJpaEntity(
						UUID.randomUUID(),
						provider,
						window,
						0,
						0,
						0,
						0,
						0,
						0,
						0,
						null,
						window));
		row.add(
				syncAttempts,
				syncSuccesses,
				syncFailures,
				evidenceAccepted,
				evidenceRejected,
				duplicates,
				adapterErrors,
				errorCategory,
				Instant.now(clock));
		jpaRepository.save(row);
	}
}
