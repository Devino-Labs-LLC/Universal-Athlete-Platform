-- V3 Slice H: durable append-only security audit (ADR-035).
-- Not athlete transparency. No UPDATE/DELETE application path.

CREATE TABLE security_audit_events (
	id BINARY(16) NOT NULL,
	event_type VARCHAR(64) NOT NULL,
	occurred_at DATETIME(6) NOT NULL,
	actor_account_id BINARY(16) NULL,
	subject_account_id BINARY(16) NULL,
	subject_athlete_id BINARY(16) NULL,
	organization_id BINARY(16) NULL,
	team_id BINARY(16) NULL,
	resource_type VARCHAR(40) NULL,
	resource_id BINARY(16) NULL,
	metadata_json VARCHAR(500) NULL,
	PRIMARY KEY (id),
	INDEX idx_security_audit_events_occurred (occurred_at),
	INDEX idx_security_audit_events_type_occurred (event_type, occurred_at),
	INDEX idx_security_audit_events_org_occurred (organization_id, occurred_at),
	INDEX idx_security_audit_events_team_occurred (team_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
