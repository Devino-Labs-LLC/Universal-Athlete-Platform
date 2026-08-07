-- Phase 7S: deterministic daily training recommendations derived from readiness assessments.
-- Guidance only. No workout mutation, adaptation proposals, or medical language.

CREATE TABLE daily_training_recommendations (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	state_date DATE NOT NULL,
	daily_readiness_assessment_id BINARY(16) NOT NULL,
	daily_athlete_state_snapshot_id BINARY(16) NOT NULL,
	daily_athlete_state_snapshot_version INT NOT NULL,
	recommendation_algorithm_version VARCHAR(64) NOT NULL,
	overall_action VARCHAR(40) NOT NULL,
	recommendation_status VARCHAR(32) NOT NULL,
	primary_reason_code VARCHAR(64) NOT NULL,
	scheduled_training_present BOOLEAN NOT NULL,
	scheduled_occurrence_count INT NOT NULL,
	modifiable_scheduled_occurrence_count INT NOT NULL,
	adjustment_count INT NOT NULL,
	limiting_dimension_count INT NOT NULL,
	generated_at DATETIME(6) NOT NULL,
	created_at DATETIME(6) NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_daily_training_rec_assessment_algorithm
		UNIQUE (daily_readiness_assessment_id, recommendation_algorithm_version),
	CONSTRAINT fk_daily_training_rec_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT fk_daily_training_rec_assessment
		FOREIGN KEY (daily_readiness_assessment_id)
			REFERENCES daily_readiness_assessments (id) ON DELETE RESTRICT,
	CONSTRAINT fk_daily_training_rec_snapshot
		FOREIGN KEY (daily_athlete_state_snapshot_id)
			REFERENCES daily_athlete_state_snapshots (id) ON DELETE RESTRICT,
	CONSTRAINT ck_daily_training_rec_snapshot_version CHECK (daily_athlete_state_snapshot_version >= 1),
	CONSTRAINT ck_daily_training_rec_action CHECK (
		overall_action IN (
			'PROCEED_AS_PLANNED',
			'MODIFY_SESSION',
			'CONSIDER_RECOVERY_SESSION',
			'NO_SCHEDULED_TRAINING',
			'INSUFFICIENT_DATA',
			'TRAINING_ALREADY_COMPLETED'
		)
	),
	CONSTRAINT ck_daily_training_rec_status CHECK (
		recommendation_status IN ('ACTIONABLE', 'INFORMATIONAL', 'INSUFFICIENT_DATA')
	),
	CONSTRAINT ck_daily_training_rec_scheduled_count CHECK (scheduled_occurrence_count >= 0),
	CONSTRAINT ck_daily_training_rec_modifiable_count CHECK (modifiable_scheduled_occurrence_count >= 0),
	CONSTRAINT ck_daily_training_rec_adjustment_count CHECK (adjustment_count >= 0),
	CONSTRAINT ck_daily_training_rec_limiting_count CHECK (limiting_dimension_count >= 0),
	INDEX idx_daily_training_rec_athlete_date (athlete_id, state_date),
	INDEX idx_daily_training_rec_athlete_action (athlete_id, overall_action),
	INDEX idx_daily_training_rec_algorithm (recommendation_algorithm_version),
	INDEX idx_daily_training_rec_assessment (daily_readiness_assessment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_training_recommendation_adjustments (
	id BINARY(16) NOT NULL,
	recommendation_id BINARY(16) NOT NULL,
	adjustment_type VARCHAR(64) NOT NULL,
	priority INT NOT NULL,
	explanation_key VARCHAR(80) NOT NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_daily_training_rec_adjustment_type
		UNIQUE (recommendation_id, adjustment_type),
	CONSTRAINT uq_daily_training_rec_adjustment_order
		UNIQUE (recommendation_id, order_index),
	CONSTRAINT fk_daily_training_rec_adjustment_parent
		FOREIGN KEY (recommendation_id) REFERENCES daily_training_recommendations (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_training_rec_adjustment_priority CHECK (priority >= 1),
	CONSTRAINT ck_daily_training_rec_adjustment_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_training_recommendation_adjustment_reasons (
	adjustment_id BINARY(16) NOT NULL,
	reason_code VARCHAR(64) NOT NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (adjustment_id, reason_code),
	CONSTRAINT uq_daily_training_rec_adj_reason_order UNIQUE (adjustment_id, order_index),
	CONSTRAINT fk_daily_training_rec_adj_reason
		FOREIGN KEY (adjustment_id) REFERENCES daily_training_recommendation_adjustments (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_training_rec_adj_reason_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_training_recommendation_adjustment_dimensions (
	adjustment_id BINARY(16) NOT NULL,
	dimension_type VARCHAR(40) NOT NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (adjustment_id, dimension_type),
	CONSTRAINT uq_daily_training_rec_adj_dim_order UNIQUE (adjustment_id, order_index),
	CONSTRAINT fk_daily_training_rec_adj_dimension
		FOREIGN KEY (adjustment_id) REFERENCES daily_training_recommendation_adjustments (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_training_rec_adj_dim_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE daily_training_recommendation_occurrences (
	recommendation_id BINARY(16) NOT NULL,
	occurrence_id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	occurrence_status VARCHAR(32) NOT NULL,
	modifiable BOOLEAN NOT NULL,
	planned_environment_name_snapshot VARCHAR(120) NULL,
	actual_environment_name_snapshot VARCHAR(120) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (recommendation_id, occurrence_id),
	CONSTRAINT uq_daily_training_rec_occ_order UNIQUE (recommendation_id, order_index),
	CONSTRAINT fk_daily_training_rec_occurrence
		FOREIGN KEY (recommendation_id) REFERENCES daily_training_recommendations (id) ON DELETE CASCADE,
	CONSTRAINT ck_daily_training_rec_occ_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
