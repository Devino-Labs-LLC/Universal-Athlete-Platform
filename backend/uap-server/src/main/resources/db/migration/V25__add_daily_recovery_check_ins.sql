-- Phase 7O: daily recovery check-ins and immutable wellness revision history.
-- Athlete-reported observations only. No readiness, fatigue score, diagnosis, or recommendations.

CREATE TABLE daily_recovery_check_ins (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	check_in_date DATE NOT NULL,
	sleep_duration_minutes INT NULL,
	sleep_quality TINYINT NULL,
	fatigue TINYINT NOT NULL,
	muscle_soreness TINYINT NOT NULL,
	stress TINYINT NOT NULL,
	mood TINYINT NOT NULL,
	motivation TINYINT NOT NULL,
	completeness VARCHAR(16) NOT NULL,
	notes VARCHAR(2000) NULL,
	source VARCHAR(32) NOT NULL,
	submitted_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_daily_recovery_check_ins_athlete_date UNIQUE (athlete_id, check_in_date),
	CONSTRAINT fk_daily_recovery_check_ins_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT ck_recovery_check_ins_sleep_duration CHECK (
		sleep_duration_minutes IS NULL
			OR (sleep_duration_minutes >= 0 AND sleep_duration_minutes <= 1440)
	),
	CONSTRAINT ck_recovery_check_ins_sleep_quality CHECK (
		sleep_quality IS NULL OR (sleep_quality BETWEEN 1 AND 5)
	),
	CONSTRAINT ck_recovery_check_ins_fatigue CHECK (fatigue BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_check_ins_soreness CHECK (muscle_soreness BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_check_ins_stress CHECK (stress BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_check_ins_mood CHECK (mood BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_check_ins_motivation CHECK (motivation BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_check_ins_version CHECK (version >= 0),
	INDEX idx_recovery_check_ins_athlete_date (athlete_id, check_in_date),
	INDEX idx_recovery_check_ins_athlete_fatigue (athlete_id, fatigue),
	INDEX idx_recovery_check_ins_athlete_soreness (athlete_id, muscle_soreness),
	INDEX idx_recovery_check_ins_athlete_completeness (athlete_id, completeness)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_recovery_check_in_discomfort (
	id BINARY(16) NOT NULL,
	recovery_check_in_id BINARY(16) NOT NULL,
	body_area VARCHAR(40) NOT NULL,
	body_side VARCHAR(32) NOT NULL,
	intensity TINYINT NOT NULL,
	notes VARCHAR(250) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_recovery_discomfort_area_side UNIQUE (recovery_check_in_id, body_area, body_side),
	CONSTRAINT fk_recovery_discomfort_check_in
		FOREIGN KEY (recovery_check_in_id) REFERENCES daily_recovery_check_ins (id) ON DELETE CASCADE,
	CONSTRAINT ck_recovery_discomfort_intensity CHECK (intensity BETWEEN 1 AND 5),
	CONSTRAINT ck_recovery_discomfort_order CHECK (order_index >= 0),
	INDEX idx_recovery_discomfort_check_in (recovery_check_in_id, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_recovery_check_in_revisions (
	id BINARY(16) NOT NULL,
	recovery_check_in_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	revision_number INT NOT NULL,
	prior_sleep_duration_minutes INT NULL,
	new_sleep_duration_minutes INT NULL,
	prior_sleep_quality TINYINT NULL,
	new_sleep_quality TINYINT NULL,
	prior_fatigue TINYINT NOT NULL,
	new_fatigue TINYINT NOT NULL,
	prior_muscle_soreness TINYINT NOT NULL,
	new_muscle_soreness TINYINT NOT NULL,
	prior_stress TINYINT NOT NULL,
	new_stress TINYINT NOT NULL,
	prior_mood TINYINT NOT NULL,
	new_mood TINYINT NOT NULL,
	prior_motivation TINYINT NOT NULL,
	new_motivation TINYINT NOT NULL,
	prior_completeness VARCHAR(16) NOT NULL,
	new_completeness VARCHAR(16) NOT NULL,
	prior_notes VARCHAR(2000) NULL,
	new_notes VARCHAR(2000) NULL,
	changed_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_recovery_check_in_revisions_number UNIQUE (recovery_check_in_id, revision_number),
	CONSTRAINT fk_recovery_revisions_check_in
		FOREIGN KEY (recovery_check_in_id) REFERENCES daily_recovery_check_ins (id) ON DELETE RESTRICT,
	CONSTRAINT fk_recovery_revisions_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT ck_recovery_revisions_number CHECK (revision_number >= 1),
	INDEX idx_recovery_revisions_check_in (recovery_check_in_id, revision_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_recovery_check_in_revision_discomfort (
	id BINARY(16) NOT NULL,
	revision_id BINARY(16) NOT NULL,
	snapshot_side VARCHAR(8) NOT NULL,
	body_area VARCHAR(40) NOT NULL,
	body_side VARCHAR(32) NOT NULL,
	intensity TINYINT NOT NULL,
	notes VARCHAR(250) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_recovery_revision_discomfort_revision
		FOREIGN KEY (revision_id) REFERENCES daily_recovery_check_in_revisions (id) ON DELETE CASCADE,
	CONSTRAINT ck_recovery_revision_discomfort_side CHECK (snapshot_side IN ('PRIOR', 'NEW')),
	CONSTRAINT ck_recovery_revision_discomfort_intensity CHECK (intensity BETWEEN 1 AND 5),
	INDEX idx_recovery_revision_discomfort_revision (revision_id, snapshot_side, order_index)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
