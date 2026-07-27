-- Phase 7H.1: canonical exercise identity.
--
-- Before this migration an exercise's performance history was keyed by the prescription row
-- (workout_exercises.id), so the same movement prescribed in two plans produced two unrelated
-- histories. exercise_definitions introduces the canonical movement, and exercise_performance_key
-- now means "exercise definition id".
--
-- Migration strategy (pre-production, deliberately NO name-based merging):
--   * Every existing workout_exercise gets its own ATHLETE_CUSTOM definition that reuses the
--     prescription's UUID as the definition id. Because the old performance key was already that
--     same UUID, every stored exercise_performance_key value is correct as-is and no key is
--     remapped. Athletes keep exactly the history they had; nothing is merged behind their backs.
--   * SYSTEM definitions are seeded with fixed UUIDs (see SystemExerciseDefinitions) so seeds,
--     application code and tests can reference the same movement without a name lookup.
--
-- Uniqueness is enforced only among active definitions, via nullable generated key columns: MySQL
-- unique indexes ignore rows whose key is NULL, so archived definitions never block a new one that
-- reuses their name.

CREATE TABLE exercise_definitions (
	id BINARY(16) NOT NULL,
	scope VARCHAR(20) NOT NULL,
	athlete_id BINARY(16) NULL,
	canonical_name VARCHAR(150) NOT NULL,
	normalized_name VARCHAR(150) NOT NULL,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	archived_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	system_active_name_key VARCHAR(150) GENERATED ALWAYS AS (
		CASE WHEN scope = 'SYSTEM' AND active = 1 THEN normalized_name ELSE NULL END
	) STORED,
	custom_active_athlete_key BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN scope = 'ATHLETE_CUSTOM' AND active = 1 THEN athlete_id ELSE NULL END
	) STORED,
	custom_active_name_key VARCHAR(150) GENERATED ALWAYS AS (
		CASE WHEN scope = 'ATHLETE_CUSTOM' AND active = 1 THEN normalized_name ELSE NULL END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_exercise_definitions_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT ck_exercise_definitions_scope_athlete CHECK (
		(scope = 'SYSTEM' AND athlete_id IS NULL)
			OR (scope = 'ATHLETE_CUSTOM' AND athlete_id IS NOT NULL)
	),
	CONSTRAINT ck_exercise_definitions_archived CHECK (
		(active = 1 AND archived_at IS NULL)
			OR (active = 0 AND archived_at IS NOT NULL)
	),
	INDEX idx_exercise_definitions_system_lookup (scope, active, canonical_name, id),
	INDEX idx_exercise_definitions_athlete_lookup (athlete_id, active, canonical_name, id),
	INDEX idx_exercise_definitions_normalized_name (normalized_name),
	INDEX idx_exercise_definitions_athlete_normalized_name (athlete_id, normalized_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO exercise_definitions (
	id, scope, athlete_id, canonical_name, normalized_name, active, archived_at,
	created_at, updated_at, version
)
VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'SYSTEM', NULL,
		'Back Squat', 'back squat', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'SYSTEM', NULL,
		'Front Squat', 'front squat', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'SYSTEM', NULL,
		'Bench Press', 'bench press', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'SYSTEM', NULL,
		'Romanian Deadlift', 'romanian deadlift', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'SYSTEM', NULL,
		'Running', 'running', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'SYSTEM', NULL,
		'Cycling', 'cycling', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111107'), 'SYSTEM', NULL,
		'Plank', 'plank', TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'SYSTEM', NULL,
		'Box Jump', 'box jump', TRUE, NULL, NOW(6), NOW(6), 0);

-- One ATHLETE_CUSTOM definition per existing prescription, reusing the prescription id so that
-- stored exercise_performance_key values already equal the new definition ids.
INSERT INTO exercise_definitions (
	id, scope, athlete_id, canonical_name, normalized_name, active, archived_at,
	created_at, updated_at, version
)
SELECT
	we.id,
	'ATHLETE_CUSTOM',
	we.athlete_id,
	LEFT(we.exercise_name, 150),
	LEFT(we.normalized_exercise_name, 150),
	TRUE,
	NULL,
	we.created_at,
	we.updated_at,
	0
FROM workout_exercises we;

-- Reusing prescription ids can produce several same-named definitions for one athlete (the same
-- movement prescribed on several days). Keep the earliest active and archive the rest so the
-- active-name uniqueness index below can be created; every definition keeps its own history.
UPDATE exercise_definitions target
JOIN (
	SELECT id, ROW_NUMBER() OVER (
		PARTITION BY athlete_id, normalized_name ORDER BY created_at, id
	) AS name_rank
	FROM exercise_definitions
	WHERE scope = 'ATHLETE_CUSTOM'
) ranked ON ranked.id = target.id
SET target.active = FALSE, target.archived_at = target.created_at
WHERE ranked.name_rank > 1;

CREATE UNIQUE INDEX uq_exercise_definitions_system_active_name
	ON exercise_definitions (system_active_name_key);

CREATE UNIQUE INDEX uq_exercise_definitions_custom_active_name
	ON exercise_definitions (custom_active_athlete_key, custom_active_name_key);

ALTER TABLE workout_exercises
	ADD COLUMN exercise_definition_id BINARY(16) NULL AFTER athlete_id;

UPDATE workout_exercises
SET exercise_definition_id = id
WHERE exercise_definition_id IS NULL;

ALTER TABLE workout_exercises
	MODIFY COLUMN exercise_definition_id BINARY(16) NOT NULL;

ALTER TABLE workout_exercises
	ADD CONSTRAINT fk_workout_exercises_exercise_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	ADD INDEX idx_workout_exercises_definition (exercise_definition_id),
	ADD INDEX idx_workout_exercises_athlete_definition (athlete_id, exercise_definition_id);

ALTER TABLE workout_exercise_executions
	ADD COLUMN exercise_definition_id BINARY(16) NULL AFTER source_workout_exercise_id;

UPDATE workout_exercise_executions
SET exercise_definition_id = exercise_performance_key
WHERE exercise_definition_id IS NULL;

ALTER TABLE workout_exercise_executions
	MODIFY COLUMN exercise_definition_id BINARY(16) NOT NULL;

ALTER TABLE workout_exercise_executions
	ADD CONSTRAINT fk_workout_exercise_executions_exercise_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	ADD INDEX idx_workout_exercise_executions_definition (exercise_definition_id),
	ADD INDEX idx_workout_exercise_executions_athlete_definition (athlete_id, exercise_definition_id);

-- Personal records and their history stay FK-free for the reasons documented in V18: they are
-- derived projections and must never be cascaded away or block a schedule change.
--
-- Their definition id is a generated column rather than a written one. The performance key IS the
-- definition id, so deriving it makes the two impossible to disagree and keeps the projection
-- writers unchanged.
ALTER TABLE athlete_exercise_personal_records
	ADD COLUMN exercise_definition_id BINARY(16)
		GENERATED ALWAYS AS (exercise_performance_key) STORED NOT NULL
		AFTER exercise_performance_key,
	ADD INDEX idx_athlete_exercise_personal_records_definition (athlete_id, exercise_definition_id);

ALTER TABLE athlete_exercise_personal_record_history
	ADD COLUMN exercise_definition_id BINARY(16)
		GENERATED ALWAYS AS (exercise_performance_key) STORED NOT NULL
		AFTER exercise_performance_key,
	ADD INDEX idx_athlete_exercise_personal_record_history_definition (athlete_id, exercise_definition_id);
