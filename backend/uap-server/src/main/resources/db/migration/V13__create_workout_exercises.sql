-- Workout exercises belonging to a workout day (prescription metadata only).
-- Unique display_order and normalized_exercise_name are enforced per day.

CREATE TABLE workout_exercises (
	id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	display_order INT NOT NULL,
	exercise_name VARCHAR(160) NOT NULL,
	normalized_exercise_name VARCHAR(160) NOT NULL,
	exercise_category VARCHAR(32) NOT NULL,
	exercise_type VARCHAR(32) NOT NULL,
	sets INT NOT NULL,
	minimum_reps INT NULL,
	maximum_reps INT NULL,
	target_weight DECIMAL(12, 4) NULL,
	weight_unit VARCHAR(16) NULL,
	target_duration_seconds INT NULL,
	target_distance DECIMAL(12, 4) NULL,
	distance_unit VARCHAR(16) NULL,
	target_rest_seconds INT NULL,
	target_rpe INT NULL,
	tempo VARCHAR(40) NULL,
	coaching_notes VARCHAR(2000) NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_exercises_workout_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_exercises_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_exercises_day_order
		UNIQUE (workout_day_id, display_order),
	CONSTRAINT uq_workout_exercises_day_name
		UNIQUE (workout_day_id, normalized_exercise_name),
	INDEX idx_workout_exercises_day_order (workout_day_id, display_order),
	INDEX idx_workout_exercises_day_name (workout_day_id, normalized_exercise_name),
	INDEX idx_workout_exercises_day_status (workout_day_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
