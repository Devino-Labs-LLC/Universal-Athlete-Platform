-- Assessment-owned measurement attachments and completion snapshots.
-- source_measurement_id is a scalar traceability reference only (no FK) so
-- completed snapshots survive source measurement deletion.

CREATE TABLE assessment_measurements (
	id BINARY(16) NOT NULL,
	assessment_id BINARY(16) NOT NULL,
	athlete_id BINARY(16) NOT NULL,
	source_measurement_id BINARY(16) NOT NULL,
	display_order INT NOT NULL,
	label VARCHAR(160) NULL,
	notes VARCHAR(1000) NULL,
	snapshot_measurement_type VARCHAR(60) NULL,
	snapshot_custom_measurement_name VARCHAR(120) NULL,
	snapshot_value DECIMAL(14,4) NULL,
	snapshot_unit VARCHAR(60) NULL,
	snapshot_custom_unit VARCHAR(60) NULL,
	snapshot_source VARCHAR(30) NULL,
	snapshot_measured_at DATETIME(6) NULL,
	snapshot_athlete_sport_id BINARY(16) NULL,
	snapshot_athlete_goal_id BINARY(16) NULL,
	snapshotted_at DATETIME(6) NULL,
	created_at DATETIME(6) NOT NULL,
	updated_at DATETIME(6) NOT NULL,
	version BIGINT NOT NULL,
	PRIMARY KEY (id),
	CONSTRAINT fk_assessment_measurements_assessment
		FOREIGN KEY (assessment_id) REFERENCES athlete_assessments (id) ON DELETE CASCADE,
	CONSTRAINT fk_assessment_measurements_athlete
		FOREIGN KEY (athlete_id) REFERENCES athletes (id),
	CONSTRAINT uq_assessment_measurements_assessment_source
		UNIQUE (assessment_id, source_measurement_id),
	INDEX idx_assessment_measurements_order (assessment_id, display_order, created_at),
	INDEX idx_assessment_measurements_athlete_assessment (athlete_id, assessment_id),
	INDEX idx_assessment_measurements_source (source_measurement_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
