-- V3 Slice B: team memberships, invitations, and MySQL-safe active uniqueness for rejoin.

-- Organization memberships: allow rejoin with a new row id after REMOVED/LEFT.
ALTER TABLE organization_memberships
	DROP INDEX uk_org_memberships_org_account;

ALTER TABLE organization_memberships
	ADD COLUMN active_account_id BINARY(16)
		GENERATED ALWAYS AS (IF(status = 'ACTIVE', account_id, NULL)) STORED,
	ADD CONSTRAINT uk_org_memberships_org_active_account
		UNIQUE (organization_id, active_account_id);

CREATE TABLE team_memberships (
	id BINARY(16) NOT NULL,
	team_id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NULL,
	role VARCHAR(30) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	active_account_id BINARY(16)
		GENERATED ALWAYS AS (IF(status = 'ACTIVE', account_id, NULL)) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_team_memberships_team
		FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
	CONSTRAINT uk_team_memberships_team_active_account
		UNIQUE (team_id, active_account_id),
	CONSTRAINT ck_team_memberships_role CHECK (
		role IN ('ATHLETE', 'COACH', 'HEAD_COACH', 'TEAM_ADMIN', 'ORG_ADMIN', 'ORG_OWNER')
	),
	CONSTRAINT ck_team_memberships_status CHECK (status IN ('ACTIVE', 'REMOVED', 'LEFT')),
	CONSTRAINT ck_team_memberships_athlete_role CHECK (
		athlete_id IS NULL OR role = 'ATHLETE'
	),
	INDEX idx_team_memberships_team_id (team_id),
	INDEX idx_team_memberships_account_id (account_id),
	INDEX idx_team_memberships_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE invitations (
	id BINARY(16) NOT NULL,
	organization_id BINARY(16) NOT NULL,
	team_id BINARY(16) NULL,
	invited_email VARCHAR(320) NOT NULL,
	invited_account_id BINARY(16) NULL,
	role VARCHAR(30) NOT NULL,
	token_hash VARCHAR(128) NOT NULL,
	status VARCHAR(20) NOT NULL,
	expires_at DATETIME(6) NOT NULL,
	accepted_membership_id BINARY(16) NULL,
	created_by_account_id BINARY(16) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	pending_dedupe_key VARCHAR(420)
		GENERATED ALWAYS AS (
			IF(
				status = 'PENDING',
				CONCAT(
					LOWER(HEX(organization_id)),
					':',
					IFNULL(LOWER(HEX(team_id)), 'ORG'),
					':',
					invited_email,
					':',
					role
				),
				NULL
			)
		) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_invitations_organization
		FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
	CONSTRAINT fk_invitations_team
		FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
	CONSTRAINT uk_invitations_token_hash UNIQUE (token_hash),
	CONSTRAINT uk_invitations_pending_dedupe UNIQUE (pending_dedupe_key),
	CONSTRAINT ck_invitations_status CHECK (
		status IN ('PENDING', 'ACCEPTED', 'DECLINED', 'REVOKED', 'EXPIRED')
	),
	CONSTRAINT ck_invitations_role CHECK (
		role IN ('ATHLETE', 'COACH', 'HEAD_COACH', 'TEAM_ADMIN', 'ORG_ADMIN', 'ORG_OWNER')
	),
	CONSTRAINT ck_invitations_scope_role CHECK (
		(team_id IS NULL AND role = 'ORG_ADMIN')
		OR (team_id IS NOT NULL AND role IN ('ATHLETE', 'COACH', 'HEAD_COACH', 'TEAM_ADMIN'))
	),
	INDEX idx_invitations_token_hash (token_hash),
	INDEX idx_invitations_organization_status (organization_id, status),
	INDEX idx_invitations_team_status (team_id, status),
	INDEX idx_invitations_email_status (invited_email, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
