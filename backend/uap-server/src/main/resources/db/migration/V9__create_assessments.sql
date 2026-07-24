-- Athlete assessment definitions (evaluation sessions).
-- Duplicate prevention for non-cancelled assessments is enforced in the application
-- layer (athlete + type + normalized_title + scheduled_at) under an athlete row lock.

CREATE TABLE athlete_assessments (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	athlete_sport_id BINARY(16) NULL,
	athlete_goal_id BINARY(16) NULL,
	assessment_type VARCHAR(40) NOT NULL,
	custom_type_name VARCHAR(120) NULL,
	title VARCHAR(160) NOT NULL,
	normalized_title VARCHAR(160) NOT NULL,
	description VARCHAR(1000) NULL,
	status VARCHAR(20) NOT NULL,
	scheduled_at DATETIME(6) NULL,
	started_at DATETIME(6) NULL,
	completed_at DATETIME(6) NULL,
	notes VARCHAR(2000) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_assessments_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_athlete_assessments_athlete_sport
		FOREIGN KEY (athlete_sport_id) REFERENCES athlete_sports (id),
	CONSTRAINT fk_athlete_assessments_athlete_goal
		FOREIGN KEY (athlete_goal_id) REFERENCES athlete_goals (id),
	INDEX idx_athlete_assessments_athlete_status (athlete_id, status),
	INDEX idx_athlete_assessments_athlete_type (athlete_id, assessment_type),
	INDEX idx_athlete_assessments_athlete_scheduled (athlete_id, scheduled_at),
	INDEX idx_athlete_assessments_duplicate_lookup (
		athlete_id, assessment_type, normalized_title, scheduled_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
