package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.connections.SyncRunView;
import com.devinolabs.uap.integrations.domain.ActiveConnectionPolicy;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.devinolabs.uap.integrations.domain.SyncRun;
import com.devinolabs.uap.integrations.domain.SyncRunId;

@Service
public class IntegrationConnectionService {

	private final ConnectionRepository connectionRepository;
	private final SyncRunRepository syncRunRepository;
	private final AthleteContextPort athleteContextPort;
	private final IntegrationsProperties properties;
	private final SecurityAuditWriter auditWriter;
	private final Clock clock;

	public IntegrationConnectionService(
			ConnectionRepository connectionRepository,
			SyncRunRepository syncRunRepository,
			AthleteContextPort athleteContextPort,
			IntegrationsProperties properties,
			SecurityAuditWriter auditWriter,
			Clock clock) {
		this.connectionRepository = Objects.requireNonNull(connectionRepository);
		this.syncRunRepository = Objects.requireNonNull(syncRunRepository);
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.properties = Objects.requireNonNull(properties);
		this.auditWriter = Objects.requireNonNull(auditWriter);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public List<ConnectionView> listConnectionsForAccount(UUID accountId) {
		AthleteRef athlete = athleteContextPort.requireAthlete(accountId);
		return connectionRepository.findByAthleteId(athlete.athleteId()).stream()
				.map(this::toView)
				.toList();
	}

	@Transactional(readOnly = true)
	public ConnectionView getConnection(UUID accountId, UUID connectionId) {
		return toView(requireOwnedConnection(accountId, connectionId, false));
	}

	@Transactional(readOnly = true)
	public List<SyncRunView> listSyncRuns(UUID accountId, UUID connectionId) {
		Connection connection = requireOwnedConnection(accountId, connectionId, false);
		return syncRunRepository.findByConnectionIdOrderByRequestedAtDesc(connection.id()).stream()
				.map(this::toSyncView)
				.toList();
	}

	@Transactional
	public ConnectionView beginConnect(UUID accountId, UUID requestId, HealthProviderKey provider) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Objects.requireNonNull(provider, "provider must not be null");
		assertModuleEnabled();
		assertProviderEnabled(provider);

		AthleteRef athlete = athleteContextPort.requireMutableAthleteForUpdate(accountId);
		ConnectionId connectionId = ConnectionId.of(requestId);

		Optional<Connection> byRequestId = connectionRepository.findById(connectionId);
		if (byRequestId.isPresent()) {
			Connection existing = byRequestId.get();
			if (!ActiveConnectionPolicy.owns(existing, accountId, athlete.athleteId())) {
				throw new IntegrationConnectionNotFoundException();
			}
			return toView(existing);
		}

		List<Connection> forAthlete = connectionRepository.findByAthleteId(athlete.athleteId());
		Optional<Connection> sameProvider = ActiveConnectionPolicy.findByProvider(forAthlete, provider);
		if (sameProvider.isPresent()) {
			Connection existing = sameProvider.get();
			if (existing.status().isActive()) {
				return toView(existing);
			}
			ActiveConnectionPolicy.assertMayActivate(forAthlete, existing.id());
			try {
				existing.reopenPending(clock);
			}
			catch (IllegalStateException ex) {
				throw new IntegrationConflictException("INTEGRATION_CONNECTION_INVALID_STATE", ex.getMessage());
			}
			Connection saved = connectionRepository.save(existing);
			audit(accountId, athlete.athleteId(), saved.id().value(), "INTEGRATION_CONNECT_BEGUN",
					"{\"provider\":\"" + provider.name() + "\",\"reopened\":true}");
			return toView(saved);
		}

		try {
			ActiveConnectionPolicy.assertMayActivate(forAthlete, null);
		}
		catch (IllegalStateException ex) {
			throw new IntegrationConflictException("INTEGRATION_ACTIVE_CONNECTION_EXISTS", ex.getMessage());
		}

		Connection created = Connection.beginPending(
				connectionId,
				athlete.athleteId(),
				accountId,
				provider,
				clock);
		Connection saved = connectionRepository.save(created);
		audit(accountId, athlete.athleteId(), saved.id().value(), "INTEGRATION_CONNECT_BEGUN",
				"{\"provider\":\"" + provider.name() + "\",\"reopened\":false}");
		return toView(saved);
	}

	@Transactional
	public ConnectionView confirmConnected(UUID accountId, UUID connectionId) {
		assertModuleEnabled();
		Connection connection = requireOwnedConnection(accountId, connectionId, true);
		try {
			connection.confirmConnected(clock);
		}
		catch (IllegalStateException ex) {
			throw new IntegrationConflictException("INTEGRATION_CONNECTION_INVALID_STATE", ex.getMessage());
		}
		Connection saved = connectionRepository.save(connection);
		audit(accountId, saved.athleteId(), saved.id().value(), "INTEGRATION_CONNECT_CONFIRMED",
				"{\"provider\":\"" + saved.provider().name() + "\"}");
		return toView(saved);
	}

	@Transactional
	public ConnectionView disconnect(UUID accountId, UUID connectionId, UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		assertModuleEnabled();
		Connection connection = requireOwnedConnection(accountId, connectionId, true);
		connection.disconnect(clock);
		Connection saved = connectionRepository.save(connection);
		audit(accountId, saved.athleteId(), saved.id().value(), "INTEGRATION_DISCONNECTED",
				"{\"provider\":\"" + saved.provider().name() + "\"}");
		return toView(saved);
	}

	@Transactional
	public SyncRunView requestSync(UUID accountId, UUID connectionId, UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		assertModuleEnabled();
		Connection connection = requireOwnedConnection(accountId, connectionId, true);
		if (!connection.acceptsSync()) {
			throw new IntegrationConflictException(
					"INTEGRATION_SYNC_NOT_ACCEPTED",
					"Sync is only accepted while the connection is CONNECTED");
		}

		SyncRun syncRun = SyncRun.request(
				SyncRunId.of(requestId),
				connection.id(),
				connection.athleteId(),
				clock);
		Optional<SyncRun> existing = syncRunRepository.findById(syncRun.id());
		if (existing.isPresent()) {
			SyncRun prior = existing.get();
			if (!prior.connectionId().equals(connection.id())
					|| !prior.athleteId().equals(connection.athleteId())) {
				throw new IntegrationConnectionNotFoundException();
			}
			return toSyncView(prior);
		}

		try {
			// OS hubs are upload-only (evidence-batch). Pull sync fails honestly — never fake SUCCEEDED.
			if (connection.provider().isOsHub()) {
				syncRun.completeOsHubUploadOnlyPipeline(clock);
			}
			else {
				syncRun.completeNoAdapterPipeline(clock);
			}
			connection.recordSyncAttempt(clock);
		}
		catch (IllegalStateException ex) {
			throw new IntegrationConflictException("INTEGRATION_SYNC_INVALID_STATE", ex.getMessage());
		}

		SyncRun savedRun = syncRunRepository.save(syncRun);
		connectionRepository.save(connection);
		audit(accountId, connection.athleteId(), connection.id().value(), "INTEGRATION_SYNC_REQUESTED",
				"{\"syncRunId\":\"" + savedRun.id().value() + "\",\"status\":\"" + savedRun.status().name()
						+ "\",\"errorCode\":\"" + (savedRun.errorCode() == null ? "" : savedRun.errorCode()) + "\"}");
		return toSyncView(savedRun);
	}

	private Connection requireOwnedConnection(UUID accountId, UUID connectionId, boolean forUpdate) {
		AthleteRef athlete = forUpdate
				? athleteContextPort.requireMutableAthleteForUpdate(accountId)
				: athleteContextPort.requireAthlete(accountId);
		Connection connection = connectionRepository.findById(ConnectionId.of(connectionId))
				.orElseThrow(IntegrationConnectionNotFoundException::new);
		if (!ActiveConnectionPolicy.owns(connection, accountId, athlete.athleteId())) {
			throw new IntegrationConnectionNotFoundException();
		}
		return connection;
	}

	private void assertModuleEnabled() {
		if (!properties.isEnabled()) {
			throw new IntegrationProviderDisabledException(
					"INTEGRATIONS_DISABLED",
					"Integrations module is disabled");
		}
	}

	private void assertProviderEnabled(HealthProviderKey provider) {
		if (!properties.isProviderEnabled(provider)) {
			throw new IntegrationProviderDisabledException(
					"INTEGRATION_PROVIDER_DISABLED",
					"Provider " + provider.name() + " is disabled");
		}
	}

	private void audit(UUID accountId, UUID athleteId, UUID resourceId, String eventType, String metadataJson) {
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				accountId,
				accountId,
				athleteId,
				null,
				null,
				"INTEGRATION_CONNECTION",
				resourceId,
				metadataJson));
	}

	private ConnectionView toView(Connection connection) {
		return new ConnectionView(
				connection.id().value(),
				connection.provider().name(),
				connection.status().name(),
				connection.processConsentGranted(),
				connection.processConsentGrantedAt(),
				connection.connectedAt(),
				connection.disconnectedAt(),
				connection.lastSuccessfulSyncAt(),
				connection.lastAttemptedSyncAt());
	}

	private SyncRunView toSyncView(SyncRun syncRun) {
		return new SyncRunView(
				syncRun.id().value(),
				syncRun.connectionId().value(),
				syncRun.status().name(),
				syncRun.requestedAt(),
				syncRun.startedAt(),
				syncRun.finishedAt(),
				syncRun.errorCode(),
				syncRun.recordsAccepted(),
				syncRun.recordsRejected());
	}

}
