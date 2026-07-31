-- Phase 7M: workout adaptation proposals with athlete review and explicit bulk apply.
-- Feasibility remains derived on read; proposals are persisted drafts for review/apply.

CREATE TABLE workout_adaptation_proposals (
	id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	training_plan_id BINARY(16) NOT NULL,
	workout_day_id BINARY(16) NOT NULL,
	workout_occurrence_id BINARY(16) NOT NULL,
	environment_context_source VARCHAR(40) NOT NULL,
	training_environment_id BINARY(16) NULL,
	environment_name_snapshot VARCHAR(100) NULL,
	occurrence_version_at_generation BIGINT NOT NULL,
	occurrence_updated_at_at_generation DATETIME(6) NULL,
	feasibility_fingerprint CHAR(64) NOT NULL,
	status VARCHAR(32) NOT NULL,
	total_executions INT NOT NULL,
	already_feasible_executions INT NOT NULL,
	proposed_substitutions INT NOT NULL,
	unresolved_executions INT NOT NULL,
	excluded_executions INT NOT NULL,
	expected_feasible_executions INT NOT NULL,
	expected_feasibility_percentage DECIMAL(7, 2) NULL,
	generated_at DATETIME(6) NOT NULL,
	expires_at DATETIME(6) NOT NULL,
	applied_at DATETIME(6) NULL,
	cancelled_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	active_occurrence_key BINARY(16) GENERATED ALWAYS AS (
		CASE
			WHEN status IN ('DRAFT', 'READY', 'PARTIALLY_RESOLVED') THEN workout_occurrence_id
			ELSE NULL
		END
	) STORED,
	PRIMARY KEY (id),
	CONSTRAINT fk_adaptation_proposals_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposals_plan
		FOREIGN KEY (training_plan_id) REFERENCES training_plans (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposals_day
		FOREIGN KEY (workout_day_id) REFERENCES workout_days (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposals_occurrence
		FOREIGN KEY (workout_occurrence_id) REFERENCES workout_occurrences (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposals_environment
		FOREIGN KEY (training_environment_id) REFERENCES training_environments (id) ON DELETE RESTRICT,
	INDEX idx_adaptation_proposals_athlete_status (athlete_id, status, generated_at),
	INDEX idx_adaptation_proposals_occurrence_status (workout_occurrence_id, status, generated_at),
	INDEX idx_adaptation_proposals_expires_at (expires_at),
	INDEX idx_adaptation_proposals_generated_at (generated_at, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE UNIQUE INDEX uq_adaptation_proposals_active_occurrence
	ON workout_adaptation_proposals (active_occurrence_key);

CREATE TABLE workout_adaptation_proposal_equipment_snapshot (
	proposal_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (proposal_id, equipment_type),
	CONSTRAINT fk_adaptation_proposal_equipment_proposal
		FOREIGN KEY (proposal_id) REFERENCES workout_adaptation_proposals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_proposal_items (
	id BINARY(16) NOT NULL,
	proposal_id BINARY(16) NOT NULL,
	workout_exercise_execution_id BINARY(16) NOT NULL,
	source_workout_exercise_id BINARY(16) NOT NULL,
	execution_order INT NOT NULL,
	prescribed_exercise_definition_id BINARY(16) NOT NULL,
	prescribed_name_snapshot VARCHAR(160) NOT NULL,
	current_performed_exercise_definition_id BINARY(16) NOT NULL,
	current_performed_name_snapshot VARCHAR(160) NOT NULL,
	exercise_performance_key_at_generation BINARY(16) NOT NULL,
	current_feasible BOOLEAN NOT NULL,
	prescribed_feasible BOOLEAN NOT NULL,
	performed_feasible BOOLEAN NOT NULL,
	analysis_reason_code VARCHAR(64) NOT NULL,
	action VARCHAR(32) NOT NULL,
	generated_target_exercise_definition_id BINARY(16) NULL,
	generated_target_name_snapshot VARCHAR(160) NULL,
	generated_relationship_id BINARY(16) NULL,
	generated_relationship_type_snapshot VARCHAR(40) NULL,
	generated_compatibility_snapshot VARCHAR(32) NULL,
	generated_rationale_snapshot VARCHAR(500) NULL,
	selected_target_exercise_definition_id BINARY(16) NULL,
	selected_relationship_id BINARY(16) NULL,
	athlete_decision VARCHAR(32) NOT NULL,
	athlete_notes VARCHAR(2000) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT uq_adaptation_proposal_items_execution UNIQUE (proposal_id, workout_exercise_execution_id),
	CONSTRAINT fk_adaptation_proposal_items_proposal
		FOREIGN KEY (proposal_id) REFERENCES workout_adaptation_proposals (id) ON DELETE CASCADE,
	CONSTRAINT fk_adaptation_proposal_items_execution
		FOREIGN KEY (workout_exercise_execution_id) REFERENCES workout_exercise_executions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_source_exercise
		FOREIGN KEY (source_workout_exercise_id) REFERENCES workout_exercises (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_prescribed_def
		FOREIGN KEY (prescribed_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_performed_def
		FOREIGN KEY (current_performed_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_generated_target
		FOREIGN KEY (generated_target_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_selected_target
		FOREIGN KEY (selected_target_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_generated_rel
		FOREIGN KEY (generated_relationship_id) REFERENCES exercise_substitution_relationships (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_proposal_items_selected_rel
		FOREIGN KEY (selected_relationship_id) REFERENCES exercise_substitution_relationships (id) ON DELETE RESTRICT,
	INDEX idx_adaptation_proposal_items_proposal_order (proposal_id, execution_order, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_proposal_item_missing_equipment (
	proposal_item_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (proposal_item_id, equipment_type),
	CONSTRAINT fk_adaptation_item_missing_equipment_item
		FOREIGN KEY (proposal_item_id) REFERENCES workout_adaptation_proposal_items (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_proposal_item_alternatives (
	id BINARY(16) NOT NULL,
	proposal_item_id BINARY(16) NOT NULL,
	rank_position INT NOT NULL,
	relationship_id BINARY(16) NULL,
	target_exercise_definition_id BINARY(16) NOT NULL,
	target_name_snapshot VARCHAR(160) NOT NULL,
	relationship_type_snapshot VARCHAR(40) NULL,
	compatibility_snapshot VARCHAR(32) NULL,
	rationale_snapshot VARCHAR(500) NULL,
	target_difficulty_snapshot VARCHAR(32) NULL,
	target_impact_level_snapshot VARCHAR(32) NULL,
	selected_default BOOLEAN NOT NULL DEFAULT FALSE,
	PRIMARY KEY (id),
	CONSTRAINT uq_adaptation_item_alternatives_rank UNIQUE (proposal_item_id, rank_position),
	CONSTRAINT fk_adaptation_item_alternatives_item
		FOREIGN KEY (proposal_item_id) REFERENCES workout_adaptation_proposal_items (id) ON DELETE CASCADE,
	CONSTRAINT fk_adaptation_item_alternatives_relationship
		FOREIGN KEY (relationship_id) REFERENCES exercise_substitution_relationships (id) ON DELETE RESTRICT,
	CONSTRAINT fk_adaptation_item_alternatives_target
		FOREIGN KEY (target_exercise_definition_id) REFERENCES exercise_definitions (id) ON DELETE RESTRICT,
	INDEX idx_adaptation_item_alternatives_item (proposal_item_id, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE workout_adaptation_proposal_item_alternative_equipment (
	alternative_id BINARY(16) NOT NULL,
	equipment_type VARCHAR(40) NOT NULL,
	PRIMARY KEY (alternative_id, equipment_type),
	CONSTRAINT fk_adaptation_alt_equipment_alternative
		FOREIGN KEY (alternative_id) REFERENCES workout_adaptation_proposal_item_alternatives (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE workout_exercise_substitution_history
	ADD COLUMN workout_adaptation_proposal_id BINARY(16) NULL AFTER training_environment_name_snapshot,
	ADD COLUMN workout_adaptation_proposal_item_id BINARY(16) NULL AFTER workout_adaptation_proposal_id,
	ADD COLUMN adaptation_decision_snapshot VARCHAR(32) NULL AFTER workout_adaptation_proposal_item_id,
	ADD CONSTRAINT fk_substitution_history_adaptation_proposal
		FOREIGN KEY (workout_adaptation_proposal_id) REFERENCES workout_adaptation_proposals (id) ON DELETE RESTRICT,
	ADD CONSTRAINT fk_substitution_history_adaptation_item
		FOREIGN KEY (workout_adaptation_proposal_item_id) REFERENCES workout_adaptation_proposal_items (id) ON DELETE RESTRICT,
	ADD INDEX idx_substitution_history_adaptation_proposal (workout_adaptation_proposal_id);
