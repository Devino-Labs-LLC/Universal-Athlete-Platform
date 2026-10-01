package com.devinolabs.uap.integrations.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.connections.SyncRunView;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;
import com.devinolabs.uap.integrations.domain.SyncRunStatus;

class IntegrationConnectionServiceTests {

	private static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final UUID ACCOUNT = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
	private static final UUID ATHLETE = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
	private static final UUID CONNECTION = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");

	private ConnectionRepository connectionRepository;
	private SyncRunRepository syncRunRepository;
	private AthleteContextPort athleteContextPort;
	private IntegrationsProperties properties;
	private SecurityAuditWriter auditWriter;
	private IntegrationConnectionService service;

	@BeforeEach
	void setUp() {
		connectionRepository = mock(ConnectionRepository.class);
		syncRunRepository = mock(SyncRunRepository.class);
		athleteContextPort = mock(AthleteContextPort.class);
		properties = new IntegrationsProperties();
		properties.setEnabled(true);
		properties.getAppleHealthkit().setEnabled(true);
		properties.getHealthConnect().setEnabled(true);
		auditWriter = mock(SecurityAuditWriter.class);
		service = new IntegrationConnectionService(
				connectionRepository,
				syncRunRepository,
				athleteContextPort,
				properties,
				auditWriter,
				CLOCK);
	}

	@Test
	void beginConnectCreatesPendingOsHubConnection() {
		UUID requestId = CONNECTION;
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(requestId))).thenReturn(Optional.empty());
		when(connectionRepository.findByAthleteId(ATHLETE)).thenReturn(List.of());
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ConnectionView view = service.beginConnect(ACCOUNT, requestId, HealthProviderKey.HEALTH_CONNECT);

		assertThat(view.provider()).isEqualTo("HEALTH_CONNECT");
		assertThat(view.lifecycleState()).isEqualTo("PENDING");
		verify(auditWriter).append(any());
	}

	@Test
	void confirmThenRequestSyncFailsHonestlyForOsHubUploadOnly() {
		Connection pending = Connection.beginPending(
				ConnectionId.of(CONNECTION),
				ATHLETE,
				ACCOUNT,
				HealthProviderKey.APPLE_HEALTHKIT,
				CLOCK);
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(CONNECTION))).thenReturn(Optional.of(pending));
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ConnectionView confirmed = service.confirmConnected(ACCOUNT, CONNECTION);
		assertThat(confirmed.lifecycleState()).isEqualTo("CONNECTED");

		UUID syncRequestId = UUID.randomUUID();
		when(syncRunRepository.findById(SyncRunId.of(syncRequestId))).thenReturn(Optional.empty());
		when(syncRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		SyncRunView sync = service.requestSync(ACCOUNT, CONNECTION, syncRequestId);

		assertThat(sync.status()).isEqualTo(SyncRunStatus.FAILED.name());
		assertThat(sync.errorCode()).isEqualTo("OS_HUB_UPLOAD_ONLY");
		verify(connectionRepository, org.mockito.Mockito.atLeastOnce()).save(any());
	}

	@Test
	void requestSyncReplaysSameRequestIdWithoutSecondSaveOfNewRun() {
		Connection connected = Connection.beginPending(
				ConnectionId.of(CONNECTION),
				ATHLETE,
				ACCOUNT,
				HealthProviderKey.HEALTH_CONNECT,
				CLOCK);
		connected.confirmConnected(CLOCK);
		UUID syncRequestId = UUID.randomUUID();
		SyncRun prior = SyncRun.request(SyncRunId.of(syncRequestId), connected.id(), ATHLETE, CLOCK);
		prior.completeOsHubUploadOnlyPipeline(CLOCK);

		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(CONNECTION))).thenReturn(Optional.of(connected));
		when(syncRunRepository.findById(SyncRunId.of(syncRequestId))).thenReturn(Optional.of(prior));

		SyncRunView replay = service.requestSync(ACCOUNT, CONNECTION, syncRequestId);

		assertThat(replay.syncRunId()).isEqualTo(syncRequestId);
		assertThat(replay.errorCode()).isEqualTo("OS_HUB_UPLOAD_ONLY");
		verify(syncRunRepository, never()).save(any());
	}

	@Test
	void disconnectStopsAcceptingSync() {
		Connection connected = Connection.beginPending(
				ConnectionId.of(CONNECTION),
				ATHLETE,
				ACCOUNT,
				HealthProviderKey.APPLE_HEALTHKIT,
				CLOCK);
		connected.confirmConnected(CLOCK);
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));
		when(connectionRepository.findById(ConnectionId.of(CONNECTION))).thenReturn(Optional.of(connected));
		when(connectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

		ConnectionView disconnected = service.disconnect(ACCOUNT, CONNECTION, UUID.randomUUID());
		assertThat(disconnected.lifecycleState()).isEqualTo("DISCONNECTED");

		assertThatThrownBy(() -> service.requestSync(ACCOUNT, CONNECTION, UUID.randomUUID()))
				.isInstanceOf(IntegrationConflictException.class)
				.hasMessageContaining("CONNECTED");
	}

	@Test
	void beginConnectRejectsWhenProviderDisabled() {
		properties.getHealthConnect().setEnabled(false);
		when(athleteContextPort.requireMutableAthleteForUpdate(ACCOUNT)).thenReturn(new AthleteRef(ATHLETE));

		assertThatThrownBy(() -> service.beginConnect(ACCOUNT, UUID.randomUUID(), HealthProviderKey.HEALTH_CONNECT))
				.isInstanceOf(IntegrationProviderDisabledException.class);
	}
}
