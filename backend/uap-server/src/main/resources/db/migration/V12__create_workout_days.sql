-- Workout days belonging to a training plan (scheduling metadata only).
-- Unique display_order and normalized_title are enforced per plan.

CREATE TABLE workout_days (
	id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	display_order INT NOT NULL,
	title VARCHAR(160) NOT NULL,
	normalized_title VARCHAR(160) NOT NULL,
	description VARCHAR(2000) NULL,
	scheduled_day VARCHAR(16) NOT NULL,
	planned_start_time TIME(6) NULL,
	expected_duration_minutes INT NULL,
	status VARCHAR(20) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_days_training_plan
		FOREIGN KEY (training_plan_id) REFERENCES training_plans (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_days_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_workout_days_plan_order
		UNIQUE (training_plan_id, display_order),
	CONSTRAINT uq_workout_days_plan_title
		UNIQUE (training_plan_id, normalized_title),
	INDEX idx_workout_days_plan_order (training_plan_id, display_order),
	INDEX idx_workout_days_plan_day (training_plan_id, scheduled_day),
	INDEX idx_workout_days_plan_status (training_plan_id, status),
	INDEX idx_workout_days_plan_title (training_plan_id, normalized_title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
