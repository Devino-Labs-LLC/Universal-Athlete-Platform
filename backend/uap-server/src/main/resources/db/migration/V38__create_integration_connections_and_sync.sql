-- V5 Slice F1: Connected Athlete connections, sync runs, and checkpoints (ADR-046/048/049).
-- Evidence tables and provider event inbox deferred to F2.
-- UNIQUE(athlete_id, provider) supports future multi-provider rows; MVP enforces at most one
-- active lifecycle ({PENDING,CONNECTED,NEEDS_REAUTH,ERROR}) per athlete in application code.

CREATE TABLE integration_connections (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	lifecycle_state VARCHAR(30) NOT NULL,
	process_consent_granted BOOLEAN NOT NULL,
	process_consent_granted_at DATETIME(6) NULL,
	scopes JSON NULL,
	connected_at DATETIME(6) NULL,
	disconnected_at DATETIME(6) NULL,
	last_successful_sync_at DATETIME(6) NULL,
	last_attempted_sync_at DATETIME(6) NULL,
	provider_user_ref VARCHAR(255) NULL,
	version BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_integration_connections_provider CHECK (
		provider IN ('APPLE_HEALTHKIT', 'HEALTH_CONNECT')
	),
	CONSTRAINT ck_integration_connections_lifecycle_state CHECK (
		lifecycle_state IN (
			'DISCONNECTED',
			'PENDING',
			'CONNECTED',
			'NEEDS_REAUTH',
			'ERROR'
		)
	),
	CONSTRAINT uk_integration_connections_athlete_provider UNIQUE (athlete_id, provider),
	INDEX idx_integration_connections_account (account_id),
	INDEX idx_integration_connections_athlete_state (athlete_id, lifecycle_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE integration_sync_runs (
	id BINARY(16) NOT NULL,
	connection_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	status VARCHAR(20) NOT NULL,
	requested_at DATETIME(6) NOT NULL,
	started_at DATETIME(6) NULL,
	finished_at DATETIME(6) NULL,
	error_code VARCHAR(64) NULL,
	records_accepted INT NOT NULL,
	records_rejected INT NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_integration_sync_runs_status CHECK (
		status IN ('REQUESTED', 'RUNNING', 'SUCCEEDED', 'PARTIAL', 'FAILED')
	),
	CONSTRAINT fk_integration_sync_runs_connection
		FOREIGN KEY (connection_id) REFERENCES integration_connections (id),
	INDEX idx_integration_sync_runs_connection (connection_id, requested_at),
	INDEX idx_integration_sync_runs_athlete (athlete_id, requested_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE integration_sync_checkpoints (
	id BINARY(16) NOT NULL,
	connection_id BINARY(16) NOT NULL,
	stream_key VARCHAR(80) NOT NULL,
	cursor_type VARCHAR(40) NOT NULL,
	cursor_value VARCHAR(512) NOT NULL,
	watermark_at DATETIME(6) NULL,
	last_attempt_at DATETIME(6) NULL,
	last_success_at DATETIME(6) NULL,
	last_error_code VARCHAR(64) NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_integration_sync_checkpoints_connection_stream UNIQUE (connection_id, stream_key),
	CONSTRAINT fk_integration_sync_checkpoints_connection
		FOREIGN KEY (connection_id) REFERENCES integration_connections (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
