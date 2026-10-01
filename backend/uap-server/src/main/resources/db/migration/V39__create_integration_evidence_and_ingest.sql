-- V5 Slice F2: Connected evidence store + integrations ingest inbox (ADR-047 / ADR-051).
-- Separate from billing_provider_events. Store-only V5A — no readiness mutation.
-- Initial ingest rejects observations older than uap.integrations.backfill-days (default 30).

CREATE TABLE integration_evidence (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	connection_id BINARY(16) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	signal_family VARCHAR(20) NOT NULL,
	signal_type VARCHAR(80) NOT NULL,
	external_record_id VARCHAR(191) NOT NULL,
	value_numeric DECIMAL(18, 6) NULL,
	value_text VARCHAR(512) NULL,
	unit_code VARCHAR(32) NOT NULL,
	period_start DATETIME(6) NULL,
	period_end DATETIME(6) NULL,
	observed_at DATETIME(6) NOT NULL,
	provider_updated_at DATETIME(6) NULL,
	ingested_at DATETIME(6) NOT NULL,
	sync_run_id BINARY(16) NULL,
	provenance_class VARCHAR(30) NOT NULL,
	source_device_or_app VARCHAR(128) NULL,
	quality_code VARCHAR(64) NULL,
	status VARCHAR(20) NOT NULL,
	version BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_integration_evidence_provider CHECK (
		provider IN ('APPLE_HEALTHKIT', 'HEALTH_CONNECT')
	),
	CONSTRAINT ck_integration_evidence_signal_family CHECK (
		signal_family IN ('SLEEP', 'ACTIVITY', 'HEART', 'HRV', 'WORKOUT')
	),
	CONSTRAINT ck_integration_evidence_provenance_class CHECK (
		provenance_class IN ('CLIENT_DEVICE', 'OS_HUB', 'OAUTH_PROVIDER')
	),
	CONSTRAINT ck_integration_evidence_status CHECK (
		status IN ('ACTIVE', 'SUPERSEDED')
	),
	CONSTRAINT uk_integration_evidence_provider_athlete_external
		UNIQUE (provider, athlete_id, external_record_id),
	CONSTRAINT fk_integration_evidence_connection
		FOREIGN KEY (connection_id) REFERENCES integration_connections (id),
	INDEX idx_integration_evidence_athlete_observed (athlete_id, observed_at),
	INDEX idx_integration_evidence_connection_observed (connection_id, observed_at),
	INDEX idx_integration_evidence_athlete_family_observed (athlete_id, signal_family, observed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE integration_ingest_events (
	id BINARY(16) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	provider_event_id VARCHAR(191) NOT NULL,
	event_type VARCHAR(64) NOT NULL,
	connection_id BINARY(16) NULL,
	athlete_id BINARY(16) NOT NULL,
	received_at DATETIME(6) NOT NULL,
	processed_at DATETIME(6) NULL,
	processing_status VARCHAR(20) NOT NULL,
	error_code VARCHAR(64) NULL,
	sync_run_id BINARY(16) NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_integration_ingest_events_provider CHECK (
		provider IN ('APPLE_HEALTHKIT', 'HEALTH_CONNECT')
	),
	CONSTRAINT ck_integration_ingest_events_processing_status CHECK (
		processing_status IN ('RECEIVED', 'PROCESSED', 'IGNORED', 'FAILED')
	),
	CONSTRAINT uk_integration_ingest_events_provider_event
		UNIQUE (provider, provider_event_id),
	CONSTRAINT fk_integration_ingest_events_connection
		FOREIGN KEY (connection_id) REFERENCES integration_connections (id),
	INDEX idx_integration_ingest_events_athlete (athlete_id, received_at),
	INDEX idx_integration_ingest_events_connection (connection_id, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
