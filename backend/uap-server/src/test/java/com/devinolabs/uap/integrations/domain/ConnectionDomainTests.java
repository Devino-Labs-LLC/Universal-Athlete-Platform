package com.devinolabs.uap.integrations.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class ConnectionDomainTests {

	private static final Instant T0 = Instant.parse("2026-09-30T16:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void beginPendingStartsWithoutProcessConsent() {
		Connection connection = pending();

		assertThat(connection.status()).isEqualTo(ConnectionStatus.PENDING);
		assertThat(connection.processConsentGranted()).isFalse();
		assertThat(connection.processConsentGrantedAt()).isNull();
		assertThat(connection.connectedAt()).isNull();
		assertThat(connection.acceptsSync()).isFalse();
	}

	@Test
	void confirmConnectedGrantsProcessConsentAndAcceptsSync() {
		Connection connection = pending();
		connection.confirmConnected(CLOCK);

		assertThat(connection.status()).isEqualTo(ConnectionStatus.CONNECTED);
		assertThat(connection.processConsentGranted()).isTrue();
		assertThat(connection.processConsentGrantedAt()).isEqualTo(T0);
		assertThat(connection.connectedAt()).isEqualTo(T0);
		assertThat(connection.acceptsSync()).isTrue();
	}

	@Test
	void confirmConnectedRejectedUnlessPending() {
		Connection connection = pending();
		connection.confirmConnected(CLOCK);

		assertThatThrownBy(() -> connection.confirmConnected(CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("PENDING");
	}

	@Test
	void disconnectClearsProviderRefAndStopsSync() {
		Connection connection = pending();
		connection.confirmConnected(CLOCK);
		connection.disconnect(CLOCK);

		assertThat(connection.status()).isEqualTo(ConnectionStatus.DISCONNECTED);
		assertThat(connection.disconnectedAt()).isEqualTo(T0);
		assertThat(connection.providerUserRef()).isNull();
		assertThat(connection.acceptsSync()).isFalse();
		assertThatThrownBy(() -> connection.recordSuccessfulSync(CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CONNECTED");
	}

	@Test
	void disconnectIsIdempotent() {
		Connection connection = pending();
		connection.disconnect(CLOCK);
		connection.disconnect(CLOCK);
		assertThat(connection.status()).isEqualTo(ConnectionStatus.DISCONNECTED);
	}

	@Test
	void needsReauthOnlyFromConnected() {
		Connection connection = pending();
		assertThatThrownBy(() -> connection.markNeedsReauth(CLOCK))
				.isInstanceOf(IllegalStateException.class);

		connection.confirmConnected(CLOCK);
		connection.markNeedsReauth(CLOCK);
		assertThat(connection.status()).isEqualTo(ConnectionStatus.NEEDS_REAUTH);
		assertThat(connection.acceptsSync()).isFalse();
	}

	@Test
	void errorOnlyFromPending() {
		Connection connection = pending();
		connection.markError(CLOCK);
		assertThat(connection.status()).isEqualTo(ConnectionStatus.ERROR);

		Connection connected = pending();
		connected.confirmConnected(CLOCK);
		assertThatThrownBy(() -> connected.markError(CLOCK))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void reopenPendingAfterDisconnect() {
		Connection connection = pending();
		connection.confirmConnected(CLOCK);
		connection.disconnect(CLOCK);
		connection.reopenPending(CLOCK);

		assertThat(connection.status()).isEqualTo(ConnectionStatus.PENDING);
		assertThat(connection.processConsentGranted()).isFalse();
		assertThat(connection.connectedAt()).isNull();
		assertThat(connection.disconnectedAt()).isNull();
	}

	@Test
	void oneActiveRuleBlocksSecondProvider() {
		UUID athleteId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		Connection apple = Connection.beginPending(
				ConnectionId.generate(), athleteId, accountId, HealthProviderKey.APPLE_HEALTHKIT, CLOCK);
		apple.confirmConnected(CLOCK);
		Connection healthConnect = Connection.beginPending(
				ConnectionId.generate(), athleteId, accountId, HealthProviderKey.HEALTH_CONNECT, CLOCK);

		assertThatThrownBy(() -> ActiveConnectionPolicy.assertMayActivate(List.of(apple, healthConnect), healthConnect.id()))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("APPLE_HEALTHKIT");
	}

	@Test
	void oneActiveAllowsAfterDisconnect() {
		UUID athleteId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		Connection apple = Connection.beginPending(
				ConnectionId.generate(), athleteId, accountId, HealthProviderKey.APPLE_HEALTHKIT, CLOCK);
		apple.confirmConnected(CLOCK);
		apple.disconnect(CLOCK);
		Connection healthConnect = Connection.beginPending(
				ConnectionId.generate(), athleteId, accountId, HealthProviderKey.HEALTH_CONNECT, CLOCK);

		ActiveConnectionPolicy.assertMayActivate(List.of(apple), healthConnect.id());
		assertThat(ActiveConnectionPolicy.findBlockingActive(List.of(apple), null)).isEmpty();
	}

	@Test
	void syncRunNoAdapterPipelineFailsHonestly() {
		SyncRun syncRun = SyncRun.request(
				SyncRunId.generate(),
				ConnectionId.generate(),
				UUID.randomUUID(),
				CLOCK);
		syncRun.completeNoAdapterPipeline(CLOCK);

		assertThat(syncRun.status()).isEqualTo(SyncRunStatus.FAILED);
		assertThat(syncRun.errorCode()).isEqualTo("NO_ADAPTER");
		assertThat(syncRun.recordsAccepted()).isZero();
		assertThat(syncRun.recordsRejected()).isZero();
		assertThat(syncRun.startedAt()).isEqualTo(T0);
		assertThat(syncRun.finishedAt()).isEqualTo(T0);
	}

	@Test
	void activeStatusesMatchAdr048() {
		assertThat(ConnectionStatus.DISCONNECTED.isActive()).isFalse();
		assertThat(ConnectionStatus.PENDING.isActive()).isTrue();
		assertThat(ConnectionStatus.CONNECTED.isActive()).isTrue();
		assertThat(ConnectionStatus.NEEDS_REAUTH.isActive()).isTrue();
		assertThat(ConnectionStatus.ERROR.isActive()).isTrue();
	}

	private static Connection pending() {
		return Connection.beginPending(
				ConnectionId.generate(),
				UUID.randomUUID(),
				UUID.randomUUID(),
				HealthProviderKey.APPLE_HEALTHKIT,
				CLOCK);
	}

}
