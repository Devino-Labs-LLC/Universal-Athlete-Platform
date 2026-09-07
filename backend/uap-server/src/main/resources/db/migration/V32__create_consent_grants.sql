-- V3 Slice C: team-scoped consent grants bound to membership generation.

CREATE TABLE consent_grants (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	team_id BINARY(16) NOT NULL,
	organization_id BINARY(16) NOT NULL,
	team_membership_id BINARY(16) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	revoked_at DATETIME(6) NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	active_membership_key BINARY(16)
		GENERATED ALWAYS AS (IF(status = 'ACTIVE', team_membership_id, NULL)) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_consent_grants_team
		FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
	CONSTRAINT fk_consent_grants_organization
		FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
	CONSTRAINT uk_consent_grants_active_membership
		UNIQUE (active_membership_key),
	CONSTRAINT ck_consent_grants_status CHECK (status IN ('ACTIVE', 'REVOKED')),
	INDEX idx_consent_grants_athlete_status (athlete_id, status),
	INDEX idx_consent_grants_team_status (team_id, status),
	INDEX idx_consent_grants_team_membership_id (team_membership_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE consent_grant_scopes (
	consent_grant_id BINARY(16) NOT NULL,
	scope VARCHAR(40) NOT NULL,
	PRIMARY KEY (consent_grant_id, scope),
	CONSTRAINT fk_consent_grant_scopes_grant
		FOREIGN KEY (consent_grant_id) REFERENCES consent_grants (id) ON DELETE CASCADE,
	CONSTRAINT ck_consent_grant_scopes_scope CHECK (
		scope IN (
			'AVAILABILITY',
			'READINESS_CATEGORY',
			'READINESS_SCORE',
			'LIMITING_DIMENSIONS',
			'RECOVERY_CHECK_IN_DETAIL',
			'TRAINING_ADHERENCE',
			'PERFORMANCE_HISTORY',
			'TRAINING_COLLABORATION',
			'EXPORT'
		)
	)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
