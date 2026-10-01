package com.devinolabs.uap.integrations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConnectedEvidenceDomainTests {

	private static final Instant T0 = Instant.parse("2026-09-30T16:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
	private static final UUID ATHLETE = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final ConnectionId CONNECTION = ConnectionId.of(
			UUID.fromString("22222222-2222-2222-2222-222222222222"));

	@Test
	void insertThenReplaySamePayloadUpdatesIdempotently() {
		ConnectedEvidence existing = sample("ext-1", new BigDecimal("420"), Instant.parse("2026-09-29T06:00:00Z"));
		ConnectedEvidence incoming = sample("ext-1", new BigDecimal("420"), Instant.parse("2026-09-29T06:00:00Z"));

		assertThat(existing.applyIncoming(incoming, CLOCK)).isEqualTo(EvidenceUpsertOutcome.UPDATED);
		assertThat(existing.valueNumeric()).isEqualByComparingTo("420");
		assertThat(existing.ingestedAt()).isEqualTo(T0);
	}

	@Test
	void newerProviderUpdatedAtReplacesValue() {
		ConnectedEvidence existing = sample(
				"ext-1",
				new BigDecimal("400"),
				Instant.parse("2026-09-28T06:00:00Z"),
				Instant.parse("2026-09-28T07:00:00Z"));
		ConnectedEvidence incoming = sample(
				"ext-1",
				new BigDecimal("450"),
				Instant.parse("2026-09-29T06:00:00Z"),
				Instant.parse("2026-09-29T07:00:00Z"));

		assertThat(existing.applyIncoming(incoming, CLOCK)).isEqualTo(EvidenceUpsertOutcome.UPDATED);
		assertThat(existing.valueNumeric()).isEqualByComparingTo("450");
		assertThat(existing.providerUpdatedAt()).isEqualTo(Instant.parse("2026-09-29T07:00:00Z"));
	}

	@Test
	void staleProviderUpdatedAtIsIgnored() {
		ConnectedEvidence existing = sample(
				"ext-1",
				new BigDecimal("450"),
				Instant.parse("2026-09-29T06:00:00Z"),
				Instant.parse("2026-09-29T07:00:00Z"));
		ConnectedEvidence incoming = sample(
				"ext-1",
				new BigDecimal("400"),
				Instant.parse("2026-09-28T06:00:00Z"),
				Instant.parse("2026-09-28T07:00:00Z"));

		assertThat(existing.applyIncoming(incoming, CLOCK)).isEqualTo(EvidenceUpsertOutcome.IGNORED_STALE);
		assertThat(existing.valueNumeric()).isEqualByComparingTo("450");
	}

	@Test
	void rejectsFutureObservedAtBeyondSkew() {
		assertThatThrownBy(() -> ConnectedEvidence.assertAcceptableObservationTime(
						T0.plusSeconds(600),
						null,
						CLOCK,
						30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("future");
	}

	@Test
	void rejectsObservedAtOlderThanBackfillWindow() {
		assertThatThrownBy(() -> ConnectedEvidence.assertAcceptableObservationTime(
						T0.minusSeconds(40L * 24 * 3600),
						null,
						CLOCK,
						30))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("backfill");
	}

	@Test
	void rejectsNegativeNumericAndInvertedPeriod() {
		assertThatThrownBy(() -> sample("ext-neg", new BigDecimal("-1"), Instant.parse("2026-09-29T06:00:00Z")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("negative");

		assertThatThrownBy(() -> ConnectedEvidence.create(
						EvidenceId.generate(),
						ATHLETE,
						CONNECTION,
						HealthProviderKey.APPLE_HEALTHKIT,
						SignalFamily.SLEEP,
						"DURATION",
						"ext-period",
						new BigDecimal("60"),
						null,
						"MINUTE",
						Instant.parse("2026-09-29T08:00:00Z"),
						Instant.parse("2026-09-29T07:00:00Z"),
						Instant.parse("2026-09-29T06:00:00Z"),
						null,
						null,
						ProvenanceClass.CLIENT_DEVICE,
						null,
						null,
						CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("periodEnd");
	}

	@Test
	void rejectsUnknownUnit() {
		assertThatThrownBy(() -> ConnectedEvidence.create(
						EvidenceId.generate(),
						ATHLETE,
						CONNECTION,
						HealthProviderKey.APPLE_HEALTHKIT,
						SignalFamily.HEART,
						"RHR",
						"ext-unit",
						new BigDecimal("52"),
						null,
						"BEATS",
						null,
						null,
						Instant.parse("2026-09-29T06:00:00Z"),
						null,
						null,
						ProvenanceClass.CLIENT_DEVICE,
						null,
						null,
						CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown unit");
	}

	@Test
	void syncRunCompletesUploadBatchAsSucceededOrPartial() {
		SyncRun run = SyncRun.request(
				SyncRunId.of(UUID.randomUUID()),
				CONNECTION,
				ATHLETE,
				CLOCK);
		run.completeUploadBatch(2, 0, CLOCK);
		assertThat(run.status()).isEqualTo(SyncRunStatus.SUCCEEDED);
		assertThat(run.recordsAccepted()).isEqualTo(2);

		SyncRun partial = SyncRun.request(
				SyncRunId.of(UUID.randomUUID()),
				CONNECTION,
				ATHLETE,
				CLOCK);
		partial.completeUploadBatch(1, 1, CLOCK);
		assertThat(partial.status()).isEqualTo(SyncRunStatus.PARTIAL);
	}

	private static ConnectedEvidence sample(String externalId, BigDecimal value, Instant observedAt) {
		return sample(externalId, value, observedAt, null);
	}

	private static ConnectedEvidence sample(
			String externalId,
			BigDecimal value,
			Instant observedAt,
			Instant providerUpdatedAt) {
		return ConnectedEvidence.create(
				EvidenceId.generate(),
				ATHLETE,
				CONNECTION,
				HealthProviderKey.APPLE_HEALTHKIT,
				SignalFamily.SLEEP,
				"DURATION",
				externalId,
				value,
				null,
				"MINUTE",
				null,
				null,
				observedAt,
				providerUpdatedAt,
				null,
				ProvenanceClass.CLIENT_DEVICE,
				"Watch",
				null,
				CLOCK);
	}
}
