-- Phase 7N: session effort, occurrence load summaries, and execution classification snapshots.
-- Training load is occurrence-historical. Session RPE is never system-inferred.

ALTER TABLE workout_exercise_executions
	ADD COLUMN performed_exercise_category_snapshot VARCHAR(32) NULL AFTER performed_exercise_name_snapshot,
	ADD COLUMN performed_primary_movement_pattern_snapshot VARCHAR(40) NULL AFTER performed_exercise_category_snapshot,
	ADD COLUMN performed_impact_level_snapshot VARCHAR(32) NULL AFTER performed_primary_movement_pattern_snapshot;

-- Backfill from current performed definition metadata (acceptable before production load reporting).
UPDATE workout_exercise_executions e
	INNER JOIN exercise_definitions d ON d.id = e.performed_exercise_definition_id
SET
	e.performed_exercise_category_snapshot = COALESCE(d.category, 'OTHER'),
	e.performed_primary_movement_pattern_snapshot = COALESCE(d.primary_movement_pattern, 'OTHER'),
	e.performed_impact_level_snapshot = COALESCE(d.impact_level, 'NO_IMPACT');

ALTER TABLE workout_exercise_executions
	MODIFY COLUMN performed_exercise_category_snapshot VARCHAR(32) NOT NULL,
	MODIFY COLUMN performed_primary_movement_pattern_snapshot VARCHAR(40) NOT NULL,
	MODIFY COLUMN performed_impact_level_snapshot VARCHAR(32) NOT NULL;

CREATE TABLE workout_session_efforts (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	session_rpe DECIMAL(3, 1) NOT NULL,
	session_duration_minutes INT NULL,
	duration_source VARCHAR(32) NOT NULL,
	perceived_notes VARCHAR(1000) NULL,
	submitted_at DATETIME(6) NOT NULL,
	effort_source VARCHAR(32) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_workout_session_efforts_occurrence UNIQUE (workout_occurrence_id),
	CONSTRAINT fk_session_efforts_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT fk_session_efforts_plan
		FOREIGN KEY (training_plan_id) REFERENCES training_plans (id) ON DELETE RESTRICT,
	CONSTRAINT fk_session_efforts_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE RESTRICT,
	CONSTRAINT fk_session_efforts_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE RESTRICT,
	CONSTRAINT ck_session_efforts_rpe CHECK (session_rpe >= 0.0 AND session_rpe <= 10.0),
	CONSTRAINT ck_session_efforts_duration CHECK (
		session_duration_minutes IS NULL
			OR (session_duration_minutes >= 1 AND session_duration_minutes <= 1440)
	),
	INDEX idx_session_efforts_athlete_submitted (athlete_id, submitted_at),
	INDEX idx_session_efforts_athlete_rpe (athlete_id, session_rpe),
	INDEX idx_session_efforts_plan (training_plan_id),
	INDEX idx_session_efforts_day (workout_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_session_effort_revisions (
	id BINARY(16) NOT NULL,
	workout_session_effort_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	revision_number INT NOT NULL,
	prior_session_rpe DECIMAL(3, 1) NOT NULL,
	new_session_rpe DECIMAL(3, 1) NOT NULL,
	prior_duration_minutes INT NULL,
	new_duration_minutes INT NULL,
	prior_notes VARCHAR(1000) NULL,
	new_notes VARCHAR(1000) NULL,
	changed_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_session_effort_revisions_number UNIQUE (workout_session_effort_id, revision_number),
	CONSTRAINT fk_session_effort_revisions_effort
		FOREIGN KEY (workout_session_effort_id) REFERENCES workout_session_efforts (id) ON DELETE RESTRICT,
	CONSTRAINT fk_session_effort_revisions_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	INDEX idx_session_effort_revisions_effort (workout_session_effort_id, revision_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_occurrence_load_summaries (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	scheduled_date DATE NOT NULL,
	session_rpe DECIMAL(3, 1) NULL,
	session_duration_minutes INT NULL,
	session_rpe_load DECIMAL(12, 2) NULL,
	prescribed_exercise_count BIGINT NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	substituted_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	skipped_set_count BIGINT NOT NULL,
	completed_repetition_count BIGINT NOT NULL,
	total_volume_kilograms DECIMAL(18, 3) NOT NULL,
	total_duration_seconds BIGINT NOT NULL,
	total_distance_meters DECIMAL(18, 3) NOT NULL,
	no_impact_exercise_count BIGINT NOT NULL,
	low_impact_exercise_count BIGINT NOT NULL,
	moderate_impact_exercise_count BIGINT NOT NULL,
	high_impact_exercise_count BIGINT NOT NULL,
	calculated_at DATETIME(6) NOT NULL,
	source_updated_at DATETIME(6) NOT NULL,
	calculation_version VARCHAR(32) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_occurrence_load_summaries_occurrence UNIQUE (workout_occurrence_id),
	CONSTRAINT fk_occurrence_load_summaries_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT fk_occurrence_load_summaries_plan
		FOREIGN KEY (training_plan_id) REFERENCES training_plans (id) ON DELETE RESTRICT,
	CONSTRAINT fk_occurrence_load_summaries_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE RESTRICT,
	CONSTRAINT fk_occurrence_load_summaries_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE RESTRICT,
	INDEX idx_occurrence_load_athlete_date (athlete_id, scheduled_date, workout_occurrence_id),
	INDEX idx_occurrence_load_athlete_calculated (athlete_id, calculated_at),
	INDEX idx_occurrence_load_plan (training_plan_id),
	INDEX idx_occurrence_load_day (workout_day_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_occurrence_load_category_summaries (
	occurrence_load_summary_id BINARY(16) NOT NULL,
	category VARCHAR(32) NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	volume_kilograms DECIMAL(18, 3) NOT NULL,
	duration_seconds BIGINT NOT NULL,
	distance_meters DECIMAL(18, 3) NOT NULL,
	PRIMARY KEY (occurrence_load_summary_id, category),
	CONSTRAINT fk_load_category_summary
		FOREIGN KEY (occurrence_load_summary_id) REFERENCES workout_occurrence_load_summaries (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_occurrence_load_movement_summaries (
	occurrence_load_summary_id BINARY(16) NOT NULL,
	primary_movement_pattern VARCHAR(40) NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	completed_repetition_count BIGINT NOT NULL,
	volume_kilograms DECIMAL(18, 3) NOT NULL,
	duration_seconds BIGINT NOT NULL,
	distance_meters DECIMAL(18, 3) NOT NULL,
	PRIMARY KEY (occurrence_load_summary_id, primary_movement_pattern),
	CONSTRAINT fk_load_movement_summary
		FOREIGN KEY (occurrence_load_summary_id) REFERENCES workout_occurrence_load_summaries (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
