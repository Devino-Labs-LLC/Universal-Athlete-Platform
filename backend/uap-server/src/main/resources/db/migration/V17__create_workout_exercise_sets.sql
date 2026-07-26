-- Phase 7G: set-level logging. Execution actuals become summaries derived from their sets, so
-- actual_rpe widens to a decimal average.

ALTER TABLE workout_exercise_executions
	MODIFY COLUMN actual_rpe DECIMAL(12, 2) NULL;

CREATE TABLE workout_exercise_sets (
	id BINARY(16) NOT NULL,
	workout_exercise_execution_id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	set_number INT NOT NULL,
	display_order INT NOT NULL,
	set_type VARCHAR(20) NOT NULL,
	prescribed_minimum_reps INT NULL,
	prescribed_maximum_reps INT NULL,
	prescribed_weight DECIMAL(12, 4) NULL,
	prescribed_weight_unit VARCHAR(16) NULL,
	prescribed_duration_seconds INT NULL,
	prescribed_distance DECIMAL(12, 4) NULL,
	prescribed_distance_unit VARCHAR(16) NULL,
	prescribed_target_rpe INT NULL,
	prescribed_rest_seconds INT NULL,
	actual_reps INT NULL,
	actual_weight DECIMAL(12, 4) NULL,
	actual_weight_unit VARCHAR(16) NULL,
	actual_duration_seconds INT NULL,
	actual_distance DECIMAL(12, 4) NULL,
	actual_distance_unit VARCHAR(16) NULL,
	actual_rest_seconds INT NULL,
	actual_rpe DECIMAL(12, 2) NULL,
	status VARCHAR(20) NOT NULL,
	started_at DATETIME(6) NULL,
	completed_at DATETIME(6) NULL,
	athlete_notes VARCHAR(2000) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_exercise_sets_execution
		FOREIGN KEY (workout_exercise_execution_id) REFERENCES workout_exercise_executions (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_exercise_sets_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_exercise_sets_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_exercise_sets_execution_number
		UNIQUE (workout_exercise_execution_id, set_number),
	CONSTRAINT uq_workout_exercise_sets_execution_order
		UNIQUE (workout_exercise_execution_id, display_order),
	CONSTRAINT ck_workout_exercise_sets_set_number CHECK (set_number >= 1),
	CONSTRAINT ck_workout_exercise_sets_display_order CHECK (display_order >= 0),
	CONSTRAINT ck_workout_exercise_sets_reps_range CHECK (
		prescribed_minimum_reps IS NULL
			OR prescribed_maximum_reps IS NULL
			OR prescribed_maximum_reps >= prescribed_minimum_reps
	),
	CONSTRAINT ck_workout_exercise_sets_prescribed_target_rpe CHECK (
		prescribed_target_rpe IS NULL OR (prescribed_target_rpe BETWEEN 0 AND 10)
	),
	CONSTRAINT ck_workout_exercise_sets_actual_rpe CHECK (
		actual_rpe IS NULL OR (actual_rpe BETWEEN 0 AND 10)
	),
	INDEX idx_workout_exercise_sets_execution_order (workout_exercise_execution_id, display_order),
	INDEX idx_workout_exercise_sets_occurrence (workout_occurrence_id),
	INDEX idx_workout_exercise_sets_athlete_status (athlete_id, status),
	INDEX idx_workout_exercise_sets_completed_at (completed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
