-- Organization / team foundation (V3 Slice A).
-- Membership roles are contextual; no created_by column and no JWT role shortcut.

CREATE TABLE organizations (
	id BINARY(16) NOT NULL,
	name VARCHAR(200) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT ck_organizations_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
	INDEX idx_organizations_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE teams (
	id BINARY(16) NOT NULL,
	organization_id BINARY(16) NOT NULL,
	name VARCHAR(200) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_teams_organization
		FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
	CONSTRAINT uk_teams_organization_name UNIQUE (organization_id, name),
	CONSTRAINT ck_teams_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
	INDEX idx_teams_organization_id (organization_id),
	INDEX idx_teams_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organization_memberships (
	id BINARY(16) NOT NULL,
	organization_id BINARY(16) NOT NULL,
	account_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NULL,
	role VARCHAR(30) NOT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_org_memberships_organization
		FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
	CONSTRAINT uk_org_memberships_org_account UNIQUE (organization_id, account_id),
	CONSTRAINT ck_org_memberships_role CHECK (
		role IN ('ATHLETE', 'COACH', 'HEAD_COACH', 'TEAM_ADMIN', 'ORG_ADMIN', 'ORG_OWNER')
	),
	CONSTRAINT ck_org_memberships_status CHECK (status IN ('ACTIVE', 'REMOVED', 'LEFT')),
	CONSTRAINT ck_org_memberships_athlete_role CHECK (
		athlete_id IS NULL OR role = 'ATHLETE'
	),
	INDEX idx_org_memberships_organization_id (organization_id),
	INDEX idx_org_memberships_account_id (account_id),
	INDEX idx_org_memberships_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
