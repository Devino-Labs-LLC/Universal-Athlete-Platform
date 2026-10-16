-- V9: devices, richer sensor families, quality, certification, ops counters.
-- Evidence uniqueness (provider, athlete_id, external_record_id) is unchanged.

ALTER TABLE integration_connections
	DROP CHECK ck_integration_connections_provider;
ALTER TABLE integration_connections
	ADD CONSTRAINT ck_integration_connections_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	);

ALTER TABLE integration_evidence
	DROP CHECK ck_integration_evidence_provider;
ALTER TABLE integration_evidence
	ADD CONSTRAINT ck_integration_evidence_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	);

ALTER TABLE integration_evidence
	DROP CHECK ck_integration_evidence_signal_family;
ALTER TABLE integration_evidence
	ADD CONSTRAINT ck_integration_evidence_signal_family CHECK (
		signal_family IN (
			'SLEEP',
			'ACTIVITY',
			'HEART',
			'HRV',
			'WORKOUT',
			'POWER',
			'CADENCE',
			'RESPIRATORY',
			'TEMPERATURE'
		)
	);

ALTER TABLE integration_evidence
	DROP CHECK ck_integration_evidence_provenance_class;
ALTER TABLE integration_evidence
	ADD CONSTRAINT ck_integration_evidence_provenance_class CHECK (
		provenance_class IN ('CLIENT_DEVICE', 'OS_HUB', 'OAUTH_PROVIDER', 'DIRECT_SENSOR')
	);

ALTER TABLE integration_ingest_events
	DROP CHECK ck_integration_ingest_events_provider;
ALTER TABLE integration_ingest_events
	ADD CONSTRAINT ck_integration_ingest_events_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	);

CREATE TABLE integration_devices (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	connection_id BINARY(16) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	external_device_id VARCHAR(191) NOT NULL,
	device_type VARCHAR(40) NOT NULL,
	manufacturer VARCHAR(80) NULL,
	model VARCHAR(80) NULL,
	display_name VARCHAR(120) NOT NULL,
	firmware_version VARCHAR(64) NULL,
	capabilities VARCHAR(512) NOT NULL,
	lifecycle_state VARCHAR(30) NOT NULL,
	first_seen_at DATETIME(6) NOT NULL,
	last_seen_at DATETIME(6) NOT NULL,
	last_successful_data_at DATETIME(6) NULL,
	version BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_integration_devices_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	),
	CONSTRAINT ck_integration_devices_type CHECK (
		device_type IN (
			'SMARTWATCH',
			'FITNESS_TRACKER',
			'HEART_RATE_MONITOR',
			'CHEST_STRAP',
			'GPS_DEVICE',
			'POWER_METER',
			'FOOT_POD',
			'SLEEP_TRACKER',
			'PERFORMANCE_SENSOR',
			'OTHER'
		)
	),
	CONSTRAINT ck_integration_devices_lifecycle CHECK (
		lifecycle_state IN ('ACTIVE', 'DISCONNECTED', 'STALE')
	),
	CONSTRAINT uk_integration_devices_athlete_provider_external
		UNIQUE (athlete_id, provider, external_device_id),
	CONSTRAINT fk_integration_devices_connection
		FOREIGN KEY (connection_id) REFERENCES integration_connections (id),
	INDEX idx_integration_devices_account (account_id),
	INDEX idx_integration_devices_connection (connection_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE integration_evidence
	ADD COLUMN device_id BINARY(16) NULL AFTER connection_id;
ALTER TABLE integration_evidence
	ADD CONSTRAINT fk_integration_evidence_device
		FOREIGN KEY (device_id) REFERENCES integration_devices (id);
CREATE INDEX idx_integration_evidence_device ON integration_evidence (device_id);

CREATE TABLE integration_provider_certification (
	provider VARCHAR(40) NOT NULL,
	certification_state VARCHAR(20) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (provider),
	CONSTRAINT ck_integration_provider_cert_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	),
	CONSTRAINT ck_integration_provider_cert_state CHECK (
		certification_state IN ('DISABLED', 'DEVELOPMENT', 'CERTIFIED')
	)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO integration_provider_certification (provider, certification_state, updated_at) VALUES
	('APPLE_HEALTHKIT', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('HEALTH_CONNECT', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('GARMIN', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('FITBIT', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('WHOOP', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('OURA', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('POLAR', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00')),
	('BLE_DIRECT', 'DEVELOPMENT', TIMESTAMP('2026-10-12 00:00:00'));

CREATE TABLE integration_ops_counters (
	id BINARY(16) NOT NULL,
	provider VARCHAR(40) NOT NULL,
	window_start DATETIME(6) NOT NULL,
	sync_attempts INT NOT NULL,
	sync_successes INT NOT NULL,
	sync_failures INT NOT NULL,
	evidence_accepted INT NOT NULL,
	evidence_rejected INT NOT NULL,
	duplicates INT NOT NULL,
	adapter_errors INT NOT NULL,
	error_category VARCHAR(64) NULL,
	updated_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uk_integration_ops_provider_window UNIQUE (provider, window_start),
	CONSTRAINT ck_integration_ops_provider CHECK (
		provider IN (
			'APPLE_HEALTHKIT',
			'HEALTH_CONNECT',
			'GARMIN',
			'FITBIT',
			'WHOOP',
			'OURA',
			'POLAR',
			'BLE_DIRECT'
		)
	)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
