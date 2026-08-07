-- Phase 7T: link immutable training recommendations to athlete-reviewed adaptation proposals.
-- Guidance provenance only. Context-only adjustments are never auto-applied as set/load mutations.

ALTER TABLE workout_adaptation_proposals
	ADD COLUMN origin VARCHAR(40) NOT NULL DEFAULT 'MANUAL' AFTER workout_occurrence_id,
	ADD COLUMN daily_training_recommendation_id BINARY(16) NULL AFTER origin,
	ADD COLUMN daily_readiness_assessment_id BINARY(16) NULL AFTER daily_training_recommendation_id,
	ADD COLUMN daily_athlete_state_snapshot_id BINARY(16) NULL AFTER daily_readiness_assessment_id,
	ADD COLUMN training_recommendation_algorithm_version VARCHAR(64) NULL AFTER daily_athlete_state_snapshot_id,
	ADD COLUMN recommendation_overall_action_snapshot VARCHAR(40) NULL AFTER training_recommendation_algorithm_version,
	ADD COLUMN recommendation_readiness_band_snapshot VARCHAR(32) NULL AFTER recommendation_overall_action_snapshot;

ALTER TABLE workout_adaptation_proposals
	ADD CONSTRAINT ck_adaptation_proposals_origin CHECK (
		origin IN ('MANUAL', 'TRAINING_RECOMMENDATION')
	),
	ADD CONSTRAINT fk_adaptation_proposals_recommendation
		FOREIGN KEY (daily_training_recommendation_id)
			REFERENCES daily_training_recommendations (id) ON DELETE RESTRICT,
	ADD CONSTRAINT fk_adaptation_proposals_readiness
		FOREIGN KEY (daily_readiness_assessment_id)
			REFERENCES daily_readiness_assessments (id) ON DELETE RESTRICT,
	ADD CONSTRAINT fk_adaptation_proposals_state_snapshot
		FOREIGN KEY (daily_athlete_state_snapshot_id)
			REFERENCES daily_athlete_state_snapshots (id) ON DELETE RESTRICT,
	ADD INDEX idx_adaptation_proposals_recommendation (daily_training_recommendation_id),
	ADD INDEX idx_adaptation_proposals_origin (athlete_id, origin, generated_at);

CREATE TABLE workout_adaptation_recommendation_adjustments (
	id BINARY(16) NOT NULL,
	proposal_id BINARY(16) NOT NULL,
	training_adjustment_type VARCHAR(64) NOT NULL,
	applicability VARCHAR(32) NOT NULL,
	explanation_key VARCHAR(80) NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_adaptation_rec_adj_type UNIQUE (proposal_id, training_adjustment_type),
	CONSTRAINT uq_adaptation_rec_adj_order UNIQUE (proposal_id, order_index),
	CONSTRAINT fk_adaptation_rec_adj_proposal
		FOREIGN KEY (proposal_id) REFERENCES workout_adaptation_proposals (id) ON DELETE CASCADE,
	CONSTRAINT ck_adaptation_rec_adj_applicability CHECK (
		applicability IN ('CONCRETELY_APPLICABLE', 'CONTEXT_ONLY', 'NOT_APPLICABLE')
	),
	CONSTRAINT ck_adaptation_rec_adj_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_recommendation_adjustment_reasons (
	adjustment_id BINARY(16) NOT NULL,
	reason_code VARCHAR(64) NOT NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (adjustment_id, reason_code),
	CONSTRAINT uq_adaptation_rec_adj_reason_order UNIQUE (adjustment_id, order_index),
	CONSTRAINT fk_adaptation_rec_adj_reason
		FOREIGN KEY (adjustment_id)
			REFERENCES workout_adaptation_recommendation_adjustments (id) ON DELETE CASCADE,
	CONSTRAINT ck_adaptation_rec_adj_reason_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_recommendation_adjustment_dimensions (
	adjustment_id BINARY(16) NOT NULL,
	dimension_type VARCHAR(40) NOT NULL,
	order_index INT NOT NULL,
	PRIMARY KEY (adjustment_id, dimension_type),
	CONSTRAINT uq_adaptation_rec_adj_dim_order UNIQUE (adjustment_id, order_index),
	CONSTRAINT fk_adaptation_rec_adj_dimension
		FOREIGN KEY (adjustment_id)
			REFERENCES workout_adaptation_recommendation_adjustments (id) ON DELETE CASCADE,
	CONSTRAINT ck_adaptation_rec_adj_dim_order CHECK (order_index >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
