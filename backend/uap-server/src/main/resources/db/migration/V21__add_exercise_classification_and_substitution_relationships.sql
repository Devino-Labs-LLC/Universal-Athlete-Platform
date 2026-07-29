-- Phase 7J: exercise classification metadata and substitution relationships.
--
-- ExerciseDefinition gains factual catalogue metadata (category, metric mode, movement, muscles,
-- equipment, laterality, kinetic chain, impact, difficulty). Multi-valued attributes live in child
-- tables so list filters stay indexed and relational.
--
-- Known SYSTEM seeds receive explicit classifications. Migrated ATHLETE_CUSTOM rows that pre-date
-- metadata receive documented placeholders (OTHER / MIXED / NOT_APPLICABLE) that athletes should
-- review; SQL deliberately does not infer anatomy from names.
--
-- Directed substitution relationships are seeded for a small deterministic set. Athletes may still
-- substitute without a relationship; relationship provenance on substitution history is optional.

ALTER TABLE exercise_definitions
	ADD COLUMN category VARCHAR(32) NULL AFTER normalized_name,
	ADD COLUMN metric_mode VARCHAR(32) NULL AFTER category,
	ADD COLUMN primary_movement_pattern VARCHAR(40) NULL AFTER metric_mode,
	ADD COLUMN laterality VARCHAR(32) NULL AFTER primary_movement_pattern,
	ADD COLUMN kinetic_chain_type VARCHAR(32) NULL AFTER laterality,
	ADD COLUMN impact_level VARCHAR(32) NULL AFTER kinetic_chain_type,
	ADD COLUMN difficulty VARCHAR(32) NULL AFTER impact_level;

CREATE TABLE exercise_definition_secondary_movement_patterns (
	exercise_definition_id BINARY(16) NOT NULL,
	movement_pattern VARCHAR(40) NOT NULL,
	PRIMARY KEY (exercise_definition_id, movement_pattern),
	CONSTRAINT fk_ex_def_secondary_movement_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_definition_primary_muscle_groups (
	exercise_definition_id BINARY(16) NOT NULL,
	muscle_group VARCHAR(40) NOT NULL,
	PRIMARY KEY (exercise_definition_id, muscle_group),
	CONSTRAINT fk_ex_def_primary_muscle_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_definition_secondary_muscle_groups (
	exercise_definition_id BINARY(16) NOT NULL,
	muscle_group VARCHAR(40) NOT NULL,
	PRIMARY KEY (exercise_definition_id, muscle_group),
	CONSTRAINT fk_ex_def_secondary_muscle_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_definition_required_equipment (
	exercise_definition_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (exercise_definition_id, equipment_type),
	CONSTRAINT fk_ex_def_required_equipment_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE exercise_definition_optional_equipment (
	exercise_definition_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (exercise_definition_id, equipment_type),
	CONSTRAINT fk_ex_def_optional_equipment_definition
		FOREIGN KEY (exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Explicit SYSTEM metadata (ids from SystemExerciseDefinitions).
UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'SQUAT',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'INTERMEDIATE'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111101');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'HAMSTRINGS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'SPINAL_ERECTORS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'BARBELL'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'SQUAT_RACK');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111101'), 'WEIGHT_PLATE');

UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'SQUAT',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'INTERMEDIATE'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111102');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'HAMSTRINGS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'ABDOMINALS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'BARBELL'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'SQUAT_RACK');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111102'), 'WEIGHT_PLATE');

UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'HORIZONTAL_PUSH',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'OPEN_CHAIN',
	impact_level = 'NO_IMPACT',
	difficulty = 'INTERMEDIATE'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111103');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'CHEST'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'TRICEPS');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'SHOULDERS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'BARBELL'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'BENCH');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111103'), 'WEIGHT_PLATE');

UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'HINGE',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'INTERMEDIATE'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111104');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'HAMSTRINGS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'SPINAL_ERECTORS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'BARBELL');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111104'), 'WEIGHT_PLATE');

-- Running / Cycling laterality convention: NOT_APPLICABLE.
UPDATE exercise_definitions SET
	category = 'ENDURANCE',
	metric_mode = 'DISTANCE_AND_DURATION',
	primary_movement_pattern = 'GAIT',
	laterality = 'NOT_APPLICABLE',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'MODERATE_IMPACT',
	difficulty = 'BEGINNER'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111105');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'CARDIORESPIRATORY');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'HAMSTRINGS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'GLUTES'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'CALVES');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'TREADMILL'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111105'), 'TRACK');

UPDATE exercise_definitions SET
	category = 'ENDURANCE',
	metric_mode = 'DISTANCE_AND_DURATION',
	primary_movement_pattern = 'LOCOMOTION',
	laterality = 'NOT_APPLICABLE',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'BEGINNER'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111106');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'CARDIORESPIRATORY');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'HAMSTRINGS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'GLUTES');
INSERT INTO exercise_definition_optional_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111106'), 'STATIONARY_BIKE');

UPDATE exercise_definitions SET
	category = 'STABILITY',
	metric_mode = 'DURATION',
	primary_movement_pattern = 'ISOMETRIC',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'NO_IMPACT',
	difficulty = 'BEGINNER'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111107');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111107'), 'ABDOMINALS');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111107'), 'SHOULDERS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111107'), 'GLUTES');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111107'), 'BODYWEIGHT');

UPDATE exercise_definitions SET
	category = 'PLYOMETRIC',
	metric_mode = 'REPETITIONS',
	primary_movement_pattern = 'JUMP',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'HIGH_IMPACT',
	difficulty = 'INTERMEDIATE'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111108');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'CALVES');
INSERT INTO exercise_definition_secondary_movement_patterns (exercise_definition_id, movement_pattern) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'LANDING');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111108'), 'PLYOMETRIC_BOX');

UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'SQUAT',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'BEGINNER'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111109');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111109'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111109'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111109'), 'HAMSTRINGS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111109'), 'DUMBBELL');

UPDATE exercise_definitions SET
	category = 'STRENGTH',
	metric_mode = 'WEIGHT_AND_REPETITIONS',
	primary_movement_pattern = 'SQUAT',
	laterality = 'BILATERAL',
	kinetic_chain_type = 'CLOSED_CHAIN',
	impact_level = 'LOW_IMPACT',
	difficulty = 'BEGINNER'
WHERE id = UUID_TO_BIN('11111111-1111-1111-1111-111111111110');

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111110'), 'QUADRICEPS'),
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111110'), 'GLUTES');
INSERT INTO exercise_definition_secondary_muscle_groups (exercise_definition_id, muscle_group) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111110'), 'HAMSTRINGS');
INSERT INTO exercise_definition_required_equipment (exercise_definition_id, equipment_type) VALUES
	(UUID_TO_BIN('11111111-1111-1111-1111-111111111110'), 'PLATE_LOADED_MACHINE');

-- Legacy placeholders for pre-existing custom definitions.
UPDATE exercise_definitions SET
	category = 'OTHER',
	metric_mode = 'MIXED',
	primary_movement_pattern = 'OTHER',
	laterality = 'NOT_APPLICABLE',
	kinetic_chain_type = 'NOT_APPLICABLE',
	impact_level = 'LOW_IMPACT',
	difficulty = 'BEGINNER'
WHERE category IS NULL;

INSERT INTO exercise_definition_primary_muscle_groups (exercise_definition_id, muscle_group)
SELECT id, 'OTHER'
FROM exercise_definitions d
WHERE NOT EXISTS (
	SELECT 1 FROM exercise_definition_primary_muscle_groups p
	WHERE p.exercise_definition_id = d.id
);

ALTER TABLE exercise_definitions
	MODIFY COLUMN category VARCHAR(32) NOT NULL,
	MODIFY COLUMN metric_mode VARCHAR(32) NOT NULL,
	MODIFY COLUMN primary_movement_pattern VARCHAR(40) NOT NULL,
	MODIFY COLUMN laterality VARCHAR(32) NOT NULL,
	MODIFY COLUMN kinetic_chain_type VARCHAR(32) NOT NULL,
	MODIFY COLUMN impact_level VARCHAR(32) NOT NULL,
	MODIFY COLUMN difficulty VARCHAR(32) NOT NULL;

CREATE INDEX idx_exercise_definitions_category ON exercise_definitions (category, active, canonical_name, id);
CREATE INDEX idx_exercise_definitions_metric_mode ON exercise_definitions (metric_mode, active);
CREATE INDEX idx_exercise_definitions_movement ON exercise_definitions (primary_movement_pattern, active);
CREATE INDEX idx_exercise_definitions_laterality ON exercise_definitions (laterality, active);
CREATE INDEX idx_exercise_definitions_impact ON exercise_definitions (impact_level, active);
CREATE INDEX idx_exercise_definitions_difficulty ON exercise_definitions (difficulty, active);

CREATE TABLE exercise_substitution_relationships (
	id BINARY(16) NOT NULL,
	owner_athlete_id BINARY(16) NULL,
	source_exercise_definition_id BINARY(16) NOT NULL,
	target_exercise_definition_id BINARY(16) NOT NULL,
	relationship_type VARCHAR(40) NOT NULL,
	compatibility_level VARCHAR(20) NOT NULL,
	rationale VARCHAR(2000) NULL,
	active BOOLEAN NOT NULL DEFAULT TRUE,
	archived_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	active_source_key BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN active = 1 THEN source_exercise_definition_id ELSE NULL END
	) STORED,
	active_target_key BINARY(16) GENERATED ALWAYS AS (
		CASE WHEN active = 1 THEN target_exercise_definition_id ELSE NULL END
	) STORED,
	active_type_key VARCHAR(40) GENERATED ALWAYS AS (
		CASE WHEN active = 1 THEN relationship_type ELSE NULL END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_ex_sub_rel_owner
		FOREIGN KEY (owner_athlete_id) REFERENCES athletes (id),
	CONSTRAINT fk_ex_sub_rel_source
		FOREIGN KEY (source_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_ex_sub_rel_target
		FOREIGN KEY (target_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT ck_ex_sub_rel_source_target CHECK (
		source_exercise_definition_id <> target_exercise_definition_id
	),
	CONSTRAINT ck_ex_sub_rel_archived CHECK (
		(active = 1 AND archived_at IS NULL)
			OR (active = 0 AND archived_at IS NOT NULL)
	),
	INDEX idx_ex_sub_rel_source_active (source_exercise_definition_id, active, compatibility_level),
	INDEX idx_ex_sub_rel_owner (owner_athlete_id, active),
	INDEX idx_ex_sub_rel_target (target_exercise_definition_id, active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uq_ex_sub_rel_active_directed
	ON exercise_substitution_relationships (active_source_key, active_target_key, active_type_key);

INSERT INTO exercise_substitution_relationships (
	id, owner_athlete_id, source_exercise_definition_id, target_exercise_definition_id,
	relationship_type, compatibility_level, rationale, active, archived_at,
	created_at, updated_at, version
) VALUES
	(UUID_TO_BIN('22222222-2222-2222-2222-222222222201'), NULL,
		UUID_TO_BIN('11111111-1111-1111-1111-111111111101'),
		UUID_TO_BIN('11111111-1111-1111-1111-111111111109'),
		'EQUIPMENT_ALTERNATIVE', 'HIGH',
		'Goblet squat when a barbell rack is unavailable',
		TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('22222222-2222-2222-2222-222222222202'), NULL,
		UUID_TO_BIN('11111111-1111-1111-1111-111111111101'),
		UUID_TO_BIN('11111111-1111-1111-1111-111111111110'),
		'EQUIPMENT_ALTERNATIVE', 'MODERATE',
		'Leg press machine alternative for back squat',
		TRUE, NULL, NOW(6), NOW(6), 0),
	(UUID_TO_BIN('22222222-2222-2222-2222-222222222203'), NULL,
		UUID_TO_BIN('11111111-1111-1111-1111-111111111102'),
		UUID_TO_BIN('11111111-1111-1111-1111-111111111109'),
		'REGRESSION', 'HIGH',
		'Goblet squat as a less demanding front-squat variation',
		TRUE, NULL, NOW(6), NOW(6), 0);

ALTER TABLE workout_exercise_substitution_history
	ADD COLUMN substitution_relationship_id BINARY(16) NULL AFTER notes,
	ADD COLUMN relationship_type_snapshot VARCHAR(40) NULL AFTER substitution_relationship_id,
	ADD COLUMN compatibility_snapshot VARCHAR(20) NULL AFTER relationship_type_snapshot,
	ADD INDEX idx_workout_exercise_substitution_history_relationship (substitution_relationship_id);
