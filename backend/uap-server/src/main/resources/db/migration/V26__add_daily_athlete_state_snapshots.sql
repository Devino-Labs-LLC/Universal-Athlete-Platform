-- Phase 7Q: immutable versioned daily athlete-state snapshots.
-- Factual training/recovery context only. No readiness, recommendations, or scores.

CREATE TABLE daily_athlete_state_snapshots (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	state_date DATE NOT NULL,
	snapshot_version INT NOT NULL,
	current_snapshot BOOLEAN NOT NULL,
	source_fingerprint CHAR(64) NOT NULL,
	generation_reason VARCHAR(40) NOT NULL,
	generated_at DATETIME(6) NOT NULL,
	completeness VARCHAR(16) NOT NULL,
	baseline_window_days INT NOT NULL,
	recovery_analytics_calculation_version VARCHAR(40) NOT NULL,
	check_in_present BOOLEAN NOT NULL,
	recovery_check_in_id BINARY(16) NULL,
	recovery_check_in_version BIGINT NULL,
	sleep_duration_minutes INT NULL,
	sleep_quality TINYINT NULL,
	fatigue TINYINT NULL,
	muscle_soreness TINYINT NULL,
	stress TINYINT NULL,
	mood TINYINT NULL,
	motivation TINYINT NULL,
	check_in_submitted_at DATETIME(6) NULL,
	check_in_last_updated_at DATETIME(6) NULL,
	occurrence_count BIGINT NOT NULL,
	completed_occurrence_count BIGINT NOT NULL,
	rated_occurrence_count BIGINT NOT NULL,
	unrated_occurrence_count BIGINT NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	completed_repetition_count BIGINT NOT NULL,
	total_volume_kilograms DECIMAL(18, 3) NOT NULL,
	total_duration_seconds BIGINT NOT NULL,
	total_distance_meters DECIMAL(18, 3) NOT NULL,
	total_session_rpe_load DECIMAL(12, 2) NULL,
	average_session_rpe DECIMAL(3, 1) NULL,
	total_session_duration_minutes BIGINT NOT NULL,
	no_impact_exercise_count BIGINT NOT NULL,
	low_impact_exercise_count BIGINT NOT NULL,
	moderate_impact_exercise_count BIGINT NOT NULL,
	high_impact_exercise_count BIGINT NOT NULL,
	scheduled_occurrence_count BIGINT NOT NULL,
	scheduled_workout_count BIGINT NOT NULL,
	completed_scheduled_count BIGINT NOT NULL,
	skipped_scheduled_count BIGINT NOT NULL,
	cancelled_scheduled_count BIGINT NOT NULL,
	in_progress_scheduled_count BIGINT NOT NULL,
	created_at DATETIME(6) NOT NULL,
	current_only_key TINYINT GENERATED ALWAYS AS (
		CASE WHEN current_snapshot THEN 1 ELSE NULL END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT uq_daily_athlete_state_version UNIQUE (athlete_id, state_date, snapshot_version),
	CONSTRAINT uq_daily_athlete_state_current UNIQUE (athlete_id, state_date, current_only_key),
	CONSTRAINT fk_daily_athlete_state_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT ck_daily_athlete_state_version CHECK (snapshot_version >= 1),
	CONSTRAINT ck_daily_athlete_state_baseline_window CHECK (baseline_window_days IN (7, 14, 28)),
	CONSTRAINT ck_daily_athlete_state_completeness CHECK (
		completeness IN ('COMPLETE', 'PARTIAL', 'MINIMAL')
	),
	INDEX idx_daily_athlete_state_athlete_date_current (athlete_id, state_date, current_snapshot),
	INDEX idx_daily_athlete_state_athlete_date_version (athlete_id, state_date, snapshot_version),
	INDEX idx_daily_athlete_state_history (athlete_id, state_date, snapshot_version)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_athlete_state_recovery_metrics (
	snapshot_id BINARY(16) NOT NULL,
	metric_type VARCHAR(40) NOT NULL,
	target_value DECIMAL(18, 4) NULL,
	metric_direction VARCHAR(40) NOT NULL,
	observation_count INT NOT NULL,
	data_sufficiency VARCHAR(16) NOT NULL,
	baseline_mean DECIMAL(18, 2) NULL,
	baseline_median DECIMAL(18, 2) NULL,
	baseline_minimum DECIMAL(18, 4) NULL,
	baseline_maximum DECIMAL(18, 4) NULL,
	baseline_standard_deviation DECIMAL(18, 4) NULL,
	absolute_difference DECIMAL(18, 4) NULL,
	percentage_difference DECIMAL(18, 4) NULL,
	standardized_deviation DECIMAL(18, 4) NULL,
	comparison_band VARCHAR(40) NOT NULL,
	reason_code VARCHAR(64) NULL,
	PRIMARY KEY (snapshot_id, metric_type),
	CONSTRAINT fk_daily_athlete_state_metrics_snapshot
		FOREIGN KEY (snapshot_id) REFERENCES daily_athlete_state_snapshots (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_athlete_state_metric_sufficiency CHECK (
		data_sufficiency IN ('INSUFFICIENT', 'LIMITED', 'SUFFICIENT')
	)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_athlete_state_discomfort (
	id BINARY(16) NOT NULL,
	snapshot_id BINARY(16) NOT NULL,
	body_area VARCHAR(40) NOT NULL,
	body_side VARCHAR(32) NOT NULL,
	intensity TINYINT NOT NULL,
	notes VARCHAR(250) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_daily_athlete_state_discomfort_snapshot
		FOREIGN KEY (snapshot_id) REFERENCES daily_athlete_state_snapshots (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_athlete_state_discomfort_intensity CHECK (intensity BETWEEN 1 AND 5),
	CONSTRAINT ck_daily_athlete_state_discomfort_order CHECK (order_index >= 0),
	INDEX idx_daily_athlete_state_discomfort_snapshot (snapshot_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_athlete_state_category_summaries (
	snapshot_id BINARY(16) NOT NULL,
	category VARCHAR(32) NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	volume_kilograms DECIMAL(18, 3) NOT NULL,
	duration_seconds BIGINT NOT NULL,
	distance_meters DECIMAL(18, 3) NOT NULL,
	PRIMARY KEY (snapshot_id, category),
	CONSTRAINT fk_daily_athlete_state_category_snapshot
		FOREIGN KEY (snapshot_id) REFERENCES daily_athlete_state_snapshots (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_athlete_state_movement_summaries (
	snapshot_id BINARY(16) NOT NULL,
	movement_pattern VARCHAR(40) NOT NULL,
	completed_exercise_count BIGINT NOT NULL,
	completed_set_count BIGINT NOT NULL,
	completed_repetition_count BIGINT NOT NULL,
	volume_kilograms DECIMAL(18, 3) NOT NULL,
	duration_seconds BIGINT NOT NULL,
	distance_meters DECIMAL(18, 3) NOT NULL,
	PRIMARY KEY (snapshot_id, movement_pattern),
	CONSTRAINT fk_daily_athlete_state_movement_snapshot
		FOREIGN KEY (snapshot_id) REFERENCES daily_athlete_state_snapshots (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_athlete_state_scheduled_occurrences (
	snapshot_id BINARY(16) NOT NULL,
	occurrence_id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	occurrence_status VARCHAR(32) NOT NULL,
	scheduled_date DATE NOT NULL,
	planned_environment_name_snapshot VARCHAR(100) NULL,
	actual_environment_name_snapshot VARCHAR(100) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (snapshot_id, occurrence_id),
	CONSTRAINT fk_daily_athlete_state_scheduled_snapshot
		FOREIGN KEY (snapshot_id) REFERENCES daily_athlete_state_snapshots (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_athlete_state_scheduled_order CHECK (order_index >= 0),
	INDEX idx_daily_athlete_state_scheduled_order (snapshot_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
