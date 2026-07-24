-- Athlete measurement history (raw observations).

CREATE TABLE athlete_measurements (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	measurement_type VARCHAR(60) NOT NULL,
	custom_measurement_name VARCHAR(120) NULL,
	measurement_value DECIMAL(14,4) NOT NULL,
	measurement_unit VARCHAR(60) NOT NULL,
	custom_unit VARCHAR(60) NULL,
	source VARCHAR(30) NOT NULL,
	notes VARCHAR(1000) NULL,
	measured_at DATETIME(6) NOT NULL,
	athlete_sport_id BINARY(16) NULL,
	athlete_goal_id BINARY(16) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_measurements_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_athlete_measurements_athlete_sport
		FOREIGN KEY (athlete_sport_id) REFERENCES athlete_sports (id),
	CONSTRAINT fk_athlete_measurements_athlete_goal
		FOREIGN KEY (athlete_goal_id) REFERENCES athlete_goals (id),
	INDEX idx_athlete_measurements_athlete_type_measured (
		athlete_id, measurement_type, measured_at),
	INDEX idx_athlete_measurements_athlete_source_measured (
		athlete_id, source, measured_at),
	INDEX idx_athlete_measurements_athlete_measured (athlete_id, measured_at),
	INDEX idx_athlete_measurements_sport_id (athlete_sport_id),
	INDEX idx_athlete_measurements_goal_id (athlete_goal_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
