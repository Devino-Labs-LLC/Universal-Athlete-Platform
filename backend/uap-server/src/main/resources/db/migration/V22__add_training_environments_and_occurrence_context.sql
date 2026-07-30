-- Phase 7K: athlete training environments and workout occurrence context snapshots.

CREATE TABLE training_environments (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	name VARCHAR(100) NOT NULL,
	normalized_name VARCHAR(100) NOT NULL,
	environment_type VARCHAR(32) NOT NULL,
	description VARCHAR(2000) NULL,
	facility_notes VARCHAR(2000) NULL,
	default_environment BOOLEAN NOT NULL DEFAULT FALSE,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	archived_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	active_athlete_key BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN active = 1 THEN athlete_id ELSE NULL END
	) STORED,
	active_name_key VARCHAR(100) GENERATED ALWAYS AS (
		CASE WHEN active = 1 THEN normalized_name ELSE NULL END
	) STORED,
	default_athlete_key BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN active = 1 AND default_environment = 1 THEN athlete_id ELSE NULL END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_training_environments_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT ck_training_environments_archived CHECK (
		(active = 1 AND archived_at IS NULL)
			OR (active = 0 AND archived_at IS NOT NULL)
	),
	CONSTRAINT ck_training_environments_default_active CHECK (
		NOT default_environment OR active = 1
	),
	INDEX idx_training_environments_athlete_active (athlete_id, active, name, id),
	INDEX idx_training_environments_type (environment_type, active),
	INDEX idx_training_environments_normalized_name (athlete_id, normalized_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uq_training_environments_active_name
	ON training_environments (active_athlete_key, active_name_key);

CREATE UNIQUE INDEX uq_training_environments_default
	ON training_environments (default_athlete_key);

CREATE TABLE training_environment_equipment (
	training_environment_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (training_environment_id, equipment_type),
	CONSTRAINT fk_training_environment_equipment_environment
		FOREIGN KEY (training_environment_id) REFERENCES training_environments (id) ON DELETE CASCADE,
	INDEX idx_training_environment_equipment_type (equipment_type, training_environment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE training_plans
	ADD COLUMN default_training_environment_id BINARY(16) NULL AFTER athlete_goal_id,
	ADD CONSTRAINT fk_training_plans_default_environment
		FOREIGN KEY (default_training_environment_id) REFERENCES training_environments (id) ON DELETE RESTRICT;

ALTER TABLE workout_days
	ADD COLUMN training_environment_override_id BINARY(16) NULL AFTER athlete_id,
	ADD CONSTRAINT fk_workout_days_environment_override
		FOREIGN KEY (training_environment_override_id) REFERENCES training_environments (id) ON DELETE RESTRICT;

ALTER TABLE workout_occurrences
	ADD COLUMN planned_training_environment_id BINARY(16) NULL AFTER athlete_notes,
	ADD COLUMN planned_training_environment_name_snapshot VARCHAR(100) NULL AFTER planned_training_environment_id,
	ADD COLUMN actual_training_environment_id BINARY(16) NULL AFTER planned_training_environment_name_snapshot,
	ADD COLUMN actual_training_environment_name_snapshot VARCHAR(100) NULL AFTER actual_training_environment_id,
	ADD COLUMN environment_selected_at DATETIME(6) NULL AFTER actual_training_environment_name_snapshot,
	ADD CONSTRAINT fk_workout_occurrences_planned_environment
		FOREIGN KEY (planned_training_environment_id) REFERENCES training_environments (id) ON DELETE RESTRICT,
	ADD CONSTRAINT fk_workout_occurrences_actual_environment
		FOREIGN KEY (actual_training_environment_id) REFERENCES training_environments (id) ON DELETE RESTRICT,
	ADD INDEX idx_workout_occurrences_planned_environment (planned_training_environment_id),
	ADD INDEX idx_workout_occurrences_actual_environment (actual_training_environment_id);

CREATE TABLE workout_occurrence_planned_equipment_snapshot (
	workout_occurrence_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (workout_occurrence_id, equipment_type),
	CONSTRAINT fk_workout_occurrence_planned_equipment_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_occurrence_actual_equipment_snapshot (
	workout_occurrence_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (workout_occurrence_id, equipment_type),
	CONSTRAINT fk_workout_occurrence_actual_equipment_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE workout_exercise_substitution_history
	ADD COLUMN training_environment_id BINARY(16) NULL AFTER compatibility_snapshot,
	ADD COLUMN training_environment_name_snapshot VARCHAR(100) NULL AFTER training_environment_id,
	ADD INDEX idx_workout_exercise_substitution_history_environment (training_environment_id);

CREATE TABLE workout_exercise_substitution_history_equipment_snapshot (
	substitution_history_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (substitution_history_id, equipment_type),
	CONSTRAINT fk_substitution_history_equipment_history
		FOREIGN KEY (substitution_history_id) REFERENCES workout_exercise_substitution_history (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
