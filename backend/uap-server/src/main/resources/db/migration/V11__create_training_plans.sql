-- Training plan definitions (structured programs assigned to athletes).
-- Soft duplicate prevention (athlete + normalized_name + overlapping dates,
-- status != ARCHIVED) is enforced in the application layer under an athlete row lock.

CREATE TABLE training_plans (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	athlete_sport_id BINARY(16) NULL,
	athlete_goal_id BINARY(16) NULL,
	name VARCHAR(160) NOT NULL,
	normalized_name VARCHAR(160) NOT NULL,
	description VARCHAR(2000) NULL,
	plan_type VARCHAR(40) NOT NULL,
	custom_type_name VARCHAR(120) NULL,
	status VARCHAR(20) NOT NULL,
	start_date DATE NOT NULL,
	end_date DATE NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_training_plans_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_training_plans_athlete_sport
		FOREIGN KEY (athlete_sport_id) REFERENCES athlete_sports (id),
	CONSTRAINT fk_training_plans_athlete_goal
		FOREIGN KEY (athlete_goal_id) REFERENCES athlete_goals (id),
	INDEX idx_training_plans_athlete_status (athlete_id, status),
	INDEX idx_training_plans_athlete_type (athlete_id, plan_type),
	INDEX idx_training_plans_athlete_dates (athlete_id, start_date, end_date),
	INDEX idx_training_plans_duplicate_lookup (
		athlete_id, normalized_name, status, start_date, end_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
