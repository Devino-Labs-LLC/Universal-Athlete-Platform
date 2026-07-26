-- Replace Phase 7D single-session model with dated occurrences and snapshotted executions.

DROP TABLE IF EXISTS workout_sessions;

CREATE TABLE workout_occurrences (
	id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	scheduled_date DATE NOT NULL,
	planned_start_time TIME(6) NULL,
	started_at DATETIME(6) NULL,
	completed_at DATETIME(6) NULL,
	status VARCHAR(20) NOT NULL,
	athlete_notes VARCHAR(4000) NULL,
	active_scheduled_date DATE GENERATED ALWAYS AS (
		CASE WHEN status = 'CANCELLED' THEN NULL ELSE scheduled_date END
	) STORED,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_occurrences_training_plan
		FOREIGN KEY (training_plan_id) REFERENCES training_plans (id),
	CONSTRAINT fk_workout_occurrences_workout_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id),
	CONSTRAINT fk_workout_occurrences_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_occurrences_day_athlete_date
		UNIQUE (workout_day_id, athlete_id, active_scheduled_date),
	INDEX idx_workout_occurrences_athlete_date (athlete_id, scheduled_date),
	INDEX idx_workout_occurrences_plan_date (training_plan_id, scheduled_date),
	INDEX idx_workout_occurrences_day_date (workout_day_id, scheduled_date),
	INDEX idx_workout_occurrences_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_exercise_executions (
	id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	source_workout_exercise_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	display_order INT NOT NULL,
	exercise_name_snapshot VARCHAR(160) NOT NULL,
	exercise_category_snapshot VARCHAR(32) NOT NULL,
	exercise_type_snapshot VARCHAR(32) NOT NULL,
	prescribed_sets INT NOT NULL,
	prescribed_minimum_reps INT NULL,
	prescribed_maximum_reps INT NULL,
	prescribed_target_weight DECIMAL(12, 4) NULL,
	prescribed_weight_unit VARCHAR(16) NULL,
	prescribed_target_duration_seconds INT NULL,
	prescribed_target_distance DECIMAL(12, 4) NULL,
	prescribed_distance_unit VARCHAR(16) NULL,
	prescribed_target_rest_seconds INT NULL,
	prescribed_target_rpe INT NULL,
	prescribed_tempo VARCHAR(40) NULL,
	prescribed_coaching_notes VARCHAR(2000) NULL,
	status VARCHAR(20) NOT NULL,
	actual_sets INT NULL,
	actual_reps INT NULL,
	actual_weight DECIMAL(12, 4) NULL,
	actual_weight_unit VARCHAR(16) NULL,
	actual_duration_seconds INT NULL,
	actual_distance DECIMAL(12, 4) NULL,
	actual_distance_unit VARCHAR(16) NULL,
	actual_rest_seconds INT NULL,
	actual_rpe INT NULL,
	started_at DATETIME(6) NULL,
	completed_at DATETIME(6) NULL,
	athlete_notes VARCHAR(4000) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_exercise_executions_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_exercise_executions_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_exercise_executions_occurrence_source
		UNIQUE (workout_occurrence_id, source_workout_exercise_id),
	CONSTRAINT uq_workout_exercise_executions_occurrence_order
		UNIQUE (workout_occurrence_id, display_order),
	INDEX idx_workout_exercise_executions_occurrence_order (workout_occurrence_id, display_order),
	INDEX idx_workout_exercise_executions_athlete_status (athlete_id, status),
	INDEX idx_workout_exercise_executions_source (source_workout_exercise_id),
	INDEX idx_workout_exercise_executions_completed_at (completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
