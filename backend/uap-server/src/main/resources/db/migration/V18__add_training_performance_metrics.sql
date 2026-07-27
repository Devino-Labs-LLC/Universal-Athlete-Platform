-- Phase 7H: training performance metrics and personal records.
--
-- Executions gain a stable exercise_performance_key so an athlete's history survives renaming or
-- deleting the prescription row. Existing rows are backfilled from source_workout_exercise_id,
-- which is exactly what new rows derive the key from.
--
-- FK policy: personal records and their history are derived projections rebuilt from completed
-- sets, so the only enforced relationship is to athletes. Set, execution and occurrence ids are
-- kept as indexed provenance references without foreign keys, which prevents an ON DELETE CASCADE
-- from silently erasing an athlete's achievement log and prevents a RESTRICT from blocking the
-- deletion of scheduled occurrences.

ALTER TABLE workout_exercise_executions
	ADD COLUMN exercise_performance_key BINARY(16) NULL AFTER source_workout_exercise_id;

UPDATE workout_exercise_executions
SET exercise_performance_key = source_workout_exercise_id
WHERE exercise_performance_key IS NULL;

ALTER TABLE workout_exercise_executions
	MODIFY COLUMN exercise_performance_key BINARY(16) NOT NULL;

ALTER TABLE workout_exercise_executions
	ADD INDEX idx_workout_exercise_executions_athlete_performance_key (athlete_id, exercise_performance_key),
	ADD INDEX idx_workout_exercise_executions_performance_key (exercise_performance_key);

CREATE TABLE athlete_exercise_personal_records (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	exercise_performance_key BINARY(16) NOT NULL,
	record_type VARCHAR(48) NOT NULL,
	record_qualifier VARCHAR(64) NULL,
	record_qualifier_key VARCHAR(64) GENERATED ALWAYS AS (COALESCE(record_qualifier, '')) STORED,
	exercise_name_snapshot VARCHAR(160) NOT NULL,
	normalized_value DECIMAL(18, 4) NOT NULL,
	normalized_unit VARCHAR(32) NOT NULL,
	measured_value DECIMAL(18, 4) NULL,
	measured_unit VARCHAR(32) NULL,
	estimated BOOLEAN NOT NULL DEFAULT FALSE,
	repetitions INT NULL,
	weight_value DECIMAL(12, 4) NULL,
	weight_unit VARCHAR(16) NULL,
	achieved_at DATETIME(6) NOT NULL,
	scheduled_date DATE NOT NULL,
	source_set_id BINARY(16) NOT NULL,
	source_execution_id BINARY(16) NOT NULL,
	source_occurrence_id BINARY(16) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_exercise_personal_records_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_athlete_exercise_personal_records_slot
		UNIQUE (athlete_id, exercise_performance_key, record_type, record_qualifier_key),
	CONSTRAINT ck_athlete_exercise_personal_records_measured CHECK (
		(measured_value IS NULL AND measured_unit IS NULL)
			OR (measured_value IS NOT NULL AND measured_unit IS NOT NULL)
	),
	CONSTRAINT ck_athlete_exercise_personal_records_weight CHECK (
		(weight_value IS NULL AND weight_unit IS NULL)
			OR (weight_value IS NOT NULL AND weight_unit IS NOT NULL)
	),
	CONSTRAINT ck_athlete_exercise_personal_records_repetitions CHECK (
		repetitions IS NULL OR repetitions >= 0
	),
	INDEX idx_athlete_exercise_personal_records_athlete_type (athlete_id, record_type),
	INDEX idx_athlete_exercise_personal_records_athlete_achieved (athlete_id, achieved_at),
	INDEX idx_athlete_exercise_personal_records_key (athlete_id, exercise_performance_key),
	INDEX idx_athlete_exercise_personal_records_source_set (source_set_id),
	INDEX idx_athlete_exercise_personal_records_source_execution (source_execution_id),
	INDEX idx_athlete_exercise_personal_records_source_occurrence (source_occurrence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE athlete_exercise_personal_record_history (
	id BINARY(16) NOT NULL,
	personal_record_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	exercise_performance_key BINARY(16) NOT NULL,
	record_type VARCHAR(48) NOT NULL,
	record_qualifier VARCHAR(64) NULL,
	exercise_name_snapshot VARCHAR(160) NOT NULL,
	normalized_value DECIMAL(18, 4) NOT NULL,
	normalized_unit VARCHAR(32) NOT NULL,
	measured_value DECIMAL(18, 4) NULL,
	measured_unit VARCHAR(32) NULL,
	estimated BOOLEAN NOT NULL DEFAULT FALSE,
	repetitions INT NULL,
	weight_value DECIMAL(12, 4) NULL,
	weight_unit VARCHAR(16) NULL,
	achieved_at DATETIME(6) NOT NULL,
	scheduled_date DATE NOT NULL,
	source_set_id BINARY(16) NOT NULL,
	source_execution_id BINARY(16) NOT NULL,
	source_occurrence_id BINARY(16) NOT NULL,
	superseded_at DATETIME(6) NULL,
	superseded_by_history_id BINARY(16) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_athlete_exercise_personal_record_history_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT ck_athlete_exercise_personal_record_history_supersession CHECK (
		(superseded_at IS NULL AND superseded_by_history_id IS NULL)
			OR (superseded_at IS NOT NULL AND superseded_by_history_id IS NOT NULL)
	),
	CONSTRAINT ck_athlete_exercise_personal_record_history_measured CHECK (
		(measured_value IS NULL AND measured_unit IS NULL)
			OR (measured_value IS NOT NULL AND measured_unit IS NOT NULL)
	),
	INDEX idx_athlete_exercise_personal_record_history_slot (
		athlete_id, exercise_performance_key, record_type, achieved_at
	),
	INDEX idx_athlete_exercise_personal_record_history_record (personal_record_id),
	INDEX idx_athlete_exercise_personal_record_history_athlete_achieved (athlete_id, achieved_at),
	INDEX idx_athlete_exercise_personal_record_history_superseded (athlete_id, superseded_at),
	INDEX idx_athlete_exercise_personal_record_history_source_set (source_set_id),
	INDEX idx_athlete_exercise_personal_record_history_source_execution (source_execution_id),
	INDEX idx_athlete_exercise_personal_record_history_source_occurrence (source_occurrence_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
