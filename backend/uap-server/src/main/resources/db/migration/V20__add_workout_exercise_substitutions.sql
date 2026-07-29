-- Phase 7I: exercise substitution and performed-movement tracking.
--
-- Until now an execution carried a single exercise_definition_id, which had to mean both "what the
-- plan asked for" and "what was trained". Those stop being the same thing as soon as an athlete
-- swaps a movement, so the column is split in two: prescribed_* is the plan snapshot and never
-- changes, performed_* follows the substitution and is what exercise_performance_key is derived
-- from. Results therefore group under the movement actually trained.
--
-- Migration strategy (pre-production, deliberately a clean split rather than an additive alias):
--   * Both new id columns are backfilled from the old exercise_definition_id, so every existing row
--     starts unsubstituted and its stored exercise_performance_key is already correct.
--   * exercise_name_snapshot is renamed to performed_exercise_name_snapshot and copied into
--     prescribed_exercise_name_snapshot, keeping the name that was displayed when the row was made.
--   * The ambiguous exercise_definition_id column is then dropped rather than left behind, so no
--     reader can accidentally group results by the prescription again.

ALTER TABLE workout_exercise_executions
	ADD COLUMN prescribed_exercise_definition_id BINARY(16) NULL AFTER source_workout_exercise_id,
	ADD COLUMN prescribed_exercise_name_snapshot VARCHAR(160) NULL AFTER prescribed_exercise_definition_id,
	ADD COLUMN performed_exercise_definition_id BINARY(16) NULL AFTER prescribed_exercise_name_snapshot,
	ADD COLUMN substitution_reason VARCHAR(32) NULL AFTER exercise_performance_key,
	ADD COLUMN substitution_notes VARCHAR(2000) NULL AFTER substitution_reason,
	ADD COLUMN substituted_at DATETIME(6) NULL AFTER substitution_notes;

ALTER TABLE workout_exercise_executions
	CHANGE COLUMN exercise_name_snapshot performed_exercise_name_snapshot VARCHAR(160) NOT NULL;

UPDATE workout_exercise_executions
SET prescribed_exercise_definition_id = exercise_definition_id,
	performed_exercise_definition_id = exercise_definition_id,
	prescribed_exercise_name_snapshot = performed_exercise_name_snapshot
WHERE prescribed_exercise_definition_id IS NULL;

ALTER TABLE workout_exercise_executions
	MODIFY COLUMN prescribed_exercise_definition_id BINARY(16) NOT NULL,
	MODIFY COLUMN prescribed_exercise_name_snapshot VARCHAR(160) NOT NULL,
	MODIFY COLUMN performed_exercise_definition_id BINARY(16) NOT NULL;

ALTER TABLE workout_exercise_executions
	DROP FOREIGN KEY fk_workout_exercise_executions_exercise_definition;

ALTER TABLE workout_exercise_executions
	DROP INDEX idx_workout_exercise_executions_definition,
	DROP INDEX idx_workout_exercise_executions_athlete_definition;

ALTER TABLE workout_exercise_executions
	DROP COLUMN exercise_definition_id;

-- RESTRICT on both sides: a definition an athlete has trained or been prescribed must stay
-- resolvable, which is why definitions are archived rather than deleted.
ALTER TABLE workout_exercise_executions
	ADD CONSTRAINT fk_workout_exercise_executions_prescribed_definition
		FOREIGN KEY (prescribed_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	ADD CONSTRAINT fk_workout_exercise_executions_performed_definition
		FOREIGN KEY (performed_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	ADD CONSTRAINT ck_workout_exercise_executions_performance_key CHECK (
		exercise_performance_key = performed_exercise_definition_id
	),
	ADD CONSTRAINT ck_workout_exercise_executions_substitution CHECK (
		(performed_exercise_definition_id = prescribed_exercise_definition_id
				AND substitution_reason IS NULL
				AND substitution_notes IS NULL
				AND substituted_at IS NULL)
			OR (performed_exercise_definition_id <> prescribed_exercise_definition_id
				AND substitution_reason IS NOT NULL
				AND substituted_at IS NOT NULL)
	),
	ADD INDEX idx_workout_exercise_executions_prescribed_definition (prescribed_exercise_definition_id),
	ADD INDEX idx_workout_exercise_executions_performed_definition (performed_exercise_definition_id),
	ADD INDEX idx_workout_exercise_executions_athlete_performed_definition (
		athlete_id, performed_exercise_definition_id
	);

CREATE TABLE workout_exercise_substitution_history (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	workout_exercise_execution_id BINARY(16) NOT NULL,
	from_exercise_definition_id BINARY(16) NOT NULL,
	from_exercise_name_snapshot VARCHAR(160) NOT NULL,
	to_exercise_definition_id BINARY(16) NOT NULL,
	to_exercise_name_snapshot VARCHAR(160) NOT NULL,
	reason VARCHAR(32) NOT NULL,
	notes VARCHAR(2000) NULL,
	reverted BOOLEAN NOT NULL DEFAULT FALSE,
	changed_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_workout_exercise_substitution_history_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_workout_exercise_substitution_history_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE CASCADE,
	CONSTRAINT fk_workout_exercise_substitution_history_execution
		FOREIGN KEY (workout_exercise_execution_id) REFERENCES workout_exercise_executions (id) ON DELETE CASCADE,
	CONSTRAINT ck_workout_exercise_substitution_history_change CHECK (
		from_exercise_definition_id <> to_exercise_definition_id
	),
	INDEX idx_workout_exercise_substitution_history_execution (workout_exercise_execution_id, changed_at, id),
	INDEX idx_workout_exercise_substitution_history_occurrence (workout_occurrence_id, changed_at),
	INDEX idx_workout_exercise_substitution_history_athlete (athlete_id, changed_at),
	INDEX idx_workout_exercise_substitution_history_from (from_exercise_definition_id),
	INDEX idx_workout_exercise_substitution_history_to (to_exercise_definition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Two more SYSTEM movements, so a squat substitution chain can be exercised entirely with shared
-- definitions instead of athlete-specific ones.
INSERT INTO exercise_definitions (
	id, scope, athlete_id, canonical_name, normalized_name, active, archived_at,
	created_at, updated_at, version
)
VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111109'), 'SYSTEM', NULL,
		'Goblet Squat', 'goblet squat', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111110'), 'SYSTEM', NULL,
		'Leg Press', 'leg press', TRUE, NULL, NOW(6), NOW(6), 0);
