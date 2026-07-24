-- Athlete goals.
-- Active-duplicate prevention is enforced in the application layer while holding a
-- PESSIMISTIC_WRITE lock on the owning athlete row (SELECT ... FOR UPDATE).
-- Index idx_athlete_goals_duplicate_lookup supports that existence check.

CREATE TABLE athlete_goals (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	goal_type VARCHAR(50) NOT NULL,
	custom_goal_name VARCHAR(120) NULL,
	title VARCHAR(160) NOT NULL,
	normalized_title VARCHAR(160) NOT NULL,
	description VARCHAR(1000) NULL,
	priority VARCHAR(20) NOT NULL,
	status VARCHAR(20) NOT NULL,
	target_value DECIMAL(12,3) NULL,
	target_unit VARCHAR(30) NULL,
	custom_target_unit VARCHAR(60) NULL,
	target_date DATE NULL,
	athlete_sport_id BINARY(16) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	completed_at DATETIME(6) NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_goals_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_athlete_goals_athlete_sport
		FOREIGN KEY (athlete_sport_id) REFERENCES athlete_sports (id),
	INDEX idx_athlete_goals_athlete_status_type (athlete_id, status, goal_type),
	INDEX idx_athlete_goals_athlete_target_date (athlete_id, target_date),
	INDEX idx_athlete_goals_athlete_sport (athlete_id, athlete_sport_id),
	INDEX idx_athlete_goals_sport_id (athlete_sport_id),
	INDEX idx_athlete_goals_duplicate_lookup (athlete_id, goal_type, normalized_title, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
