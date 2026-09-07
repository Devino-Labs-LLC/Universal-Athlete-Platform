-- V3 Slice E: athlete-owned coach assignment provenance and response.
-- Not a second training-plan tree. Does not store readiness or State Engine output.

CREATE TABLE training_assignments (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	team_id BINARY(16) NOT NULL,
	organization_id BINARY(16) NOT NULL,
	athlete_membership_id BINARY(16) NOT NULL,
	assigned_by_account_id BINARY(16) NOT NULL,
	assigned_by_role VARCHAR(20) NOT NULL,
	title VARCHAR(160) NOT NULL,
	description VARCHAR(2000) NULL,
	scheduled_date DATE NOT NULL,
	status VARCHAR(20) NOT NULL,
	athlete_response_note VARCHAR(500) NULL,
	responded_at DATETIME(6) NULL,
	idempotency_key VARCHAR(80) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_training_assignments_team
		FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE RESTRICT,
	CONSTRAINT fk_training_assignments_organization
		FOREIGN KEY (organization_id) REFERENCES organizations (id) ON DELETE RESTRICT,
	CONSTRAINT ck_training_assignments_status
		CHECK (status IN ('ASSIGNED', 'DECLINED', 'UNABLE')),
	CONSTRAINT ck_training_assignments_role
		CHECK (assigned_by_role IN ('COACH', 'HEAD_COACH')),
	CONSTRAINT uk_training_assignments_actor_idempotency
		UNIQUE (assigned_by_account_id, team_id, athlete_id, idempotency_key),
	INDEX idx_training_assignments_athlete_date (athlete_id, scheduled_date),
	INDEX idx_training_assignments_team_athlete (team_id, athlete_id, scheduled_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
