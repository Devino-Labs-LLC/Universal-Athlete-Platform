-- Phase 7R: deterministic daily readiness assessments derived from immutable athlete-state snapshots.
-- Training-context decision support only. No medical clearance, injury prediction, or recommendations.

CREATE TABLE daily_readiness_assessments (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	state_date DATE NOT NULL,
	daily_athlete_state_snapshot_id BINARY(16) NOT NULL,
	daily_athlete_state_snapshot_version INT NOT NULL,
	algorithm_version VARCHAR(40) NOT NULL,
	readiness_score DECIMAL(5, 2) NULL,
	readiness_band VARCHAR(32) NOT NULL,
	data_sufficiency VARCHAR(16) NOT NULL,
	summary_reason_code VARCHAR(64) NOT NULL,
	limiting_dimension_count INT NOT NULL,
	contributing_dimension_count INT NOT NULL,
	assessed_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_daily_readiness_snapshot_algorithm
		UNIQUE (daily_athlete_state_snapshot_id, algorithm_version),
	CONSTRAINT fk_daily_readiness_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT fk_daily_readiness_snapshot
		FOREIGN KEY (daily_athlete_state_snapshot_id)
			REFERENCES daily_athlete_state_snapshots (id) ON DELETE RESTRICT,
	CONSTRAINT ck_daily_readiness_snapshot_version CHECK (daily_athlete_state_snapshot_version >= 1),
	CONSTRAINT ck_daily_readiness_score CHECK (
		readiness_score IS NULL
			OR (readiness_score >= 0.00 AND readiness_score <= 100.00)
	),
	CONSTRAINT ck_daily_readiness_band CHECK (
		readiness_band IN ('HIGH', 'MODERATE', 'LOW', 'INSUFFICIENT_DATA')
	),
	CONSTRAINT ck_daily_readiness_sufficiency CHECK (
		data_sufficiency IN ('INSUFFICIENT', 'LIMITED', 'SUFFICIENT')
	),
	CONSTRAINT ck_daily_readiness_limiting_count CHECK (limiting_dimension_count >= 0),
	CONSTRAINT ck_daily_readiness_contributing_count CHECK (contributing_dimension_count >= 0),
	INDEX idx_daily_readiness_athlete_date (athlete_id, state_date),
	INDEX idx_daily_readiness_athlete_band (athlete_id, readiness_band),
	INDEX idx_daily_readiness_algorithm (algorithm_version),
	INDEX idx_daily_readiness_snapshot (daily_athlete_state_snapshot_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_readiness_dimension_contributions (
	assessment_id BINARY(16) NOT NULL,
	dimension_type VARCHAR(40) NOT NULL,
	source_metric_type VARCHAR(40) NULL,
	available BOOLEAN NOT NULL,
	baseline_sufficiency VARCHAR(16) NULL,
	target_value DECIMAL(18, 4) NULL,
	baseline_mean DECIMAL(18, 2) NULL,
	standardized_deviation DECIMAL(18, 4) NULL,
	comparison_band VARCHAR(40) NULL,
	normalized_score DECIMAL(5, 2) NULL,
	configured_weight DECIMAL(6, 5) NOT NULL,
	effective_weight DECIMAL(6, 5) NULL,
	weighted_contribution DECIMAL(8, 4) NULL,
	reason_code VARCHAR(64) NOT NULL,
	rank_as_limiting INT NULL,
	rank_as_strongest INT NULL,
	PRIMARY KEY (assessment_id, dimension_type),
	CONSTRAINT fk_daily_readiness_dimension_assessment
		FOREIGN KEY (assessment_id) REFERENCES daily_readiness_assessments (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_readiness_dimension_normalized CHECK (
		normalized_score IS NULL
			OR (normalized_score >= 0.00 AND normalized_score <= 100.00)
	),
	CONSTRAINT ck_daily_readiness_dimension_configured_weight CHECK (configured_weight >= 0),
	CONSTRAINT ck_daily_readiness_dimension_effective_weight CHECK (
		effective_weight IS NULL OR effective_weight >= 0
	),
	INDEX idx_daily_readiness_dimension_assessment (assessment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_readiness_limiting_dimensions (
	assessment_id BINARY(16) NOT NULL,
	dimension_type VARCHAR(40) NOT NULL,
	rank_order INT NOT NULL,
	PRIMARY KEY (assessment_id, dimension_type),
	CONSTRAINT uq_daily_readiness_limiting_rank UNIQUE (assessment_id, rank_order),
	CONSTRAINT fk_daily_readiness_limiting_assessment
		FOREIGN KEY (assessment_id) REFERENCES daily_readiness_assessments (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_readiness_limiting_rank CHECK (rank_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_readiness_strongest_dimensions (
	assessment_id BINARY(16) NOT NULL,
	dimension_type VARCHAR(40) NOT NULL,
	rank_order INT NOT NULL,
	PRIMARY KEY (assessment_id, dimension_type),
	CONSTRAINT uq_daily_readiness_strongest_rank UNIQUE (assessment_id, rank_order),
	CONSTRAINT fk_daily_readiness_strongest_assessment
		FOREIGN KEY (assessment_id) REFERENCES daily_readiness_assessments (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_readiness_strongest_rank CHECK (rank_order >= 1)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
