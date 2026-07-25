-- Workout sessions: one execution record per workout exercise.

CREATE TABLE workout_sessions (
	id BINARY(16) NOT NULL,
	workout_exercise_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	status VARCHAR(20) NOT NULL,
	actual_sets INT NULL,
	actual_reps INT NULL,
	actual_weight DECIMAL(12, 4) NULL,
	weight_unit VARCHAR(16) NULL,
	actual_duration_seconds INT NULL,
	actual_distance DECIMAL(12, 4) NULL,
	distance_unit VARCHAR(16) NULL,
	actual_rest_seconds INT NULL,
	actual_rpe INT NULL,
	completed_at DATETIME(6) NULL,
	athlete_notes VARCHAR(4000) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_sessions_workout_exercise
		FOREIGN KEY (workout_exercise_id) REFERENCES workout_exercises (id),
	CONSTRAINT fk_workout_sessions_workout_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id),
	CONSTRAINT fk_workout_sessions_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_sessions_exercise
		UNIQUE (workout_exercise_id),
	INDEX idx_workout_sessions_athlete (athlete_id),
	INDEX idx_workout_sessions_status (status),
	INDEX idx_workout_sessions_completed_at (completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
