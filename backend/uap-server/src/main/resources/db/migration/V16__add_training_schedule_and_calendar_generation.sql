-- Phase 7F: plan-level scheduling, deterministic occurrence generation, athlete calendar.

ALTER TABLE training_plans
	ADD COLUMN schedule_start_date DATE NULL,
	ADD COLUMN schedule_end_date DATE NULL,
	ADD COLUMN schedule_timezone VARCHAR(64) NULL,
	ADD COLUMN schedule_status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
	ADD COLUMN recurrence_mode VARCHAR(20) NULL,
	ADD COLUMN schedule_generated_through DATE NULL,
	ADD COLUMN schedule_activated_at DATETIME(6) NULL,
	ADD COLUMN schedule_paused_at DATETIME(6) NULL;

CREATE INDEX idx_training_plans_athlete_schedule_status
	ON training_plans (athlete_id, schedule_status);
CREATE INDEX idx_training_plans_schedule_start_date
	ON training_plans (schedule_start_date);
CREATE INDEX idx_training_plans_schedule_generated_through
	ON training_plans (schedule_generated_through);

-- scheduled_day carried no week context; rename it and pair it with the plan week it belongs to.
ALTER TABLE workout_days
	CHANGE COLUMN scheduled_day scheduled_day_of_week VARCHAR(16) NOT NULL,
	ADD COLUMN plan_week_number INT NULL;

-- MySQL treats NULLs as distinct in unique indexes, so a plain composite unique key would not
-- reject duplicate placements that share a NULL planned_start_time. placement_key folds the whole
-- placement into one deterministic string and stays NULL for pre-Phase-7F rows that have no week.
ALTER TABLE workout_days
	ADD COLUMN placement_key VARCHAR(64) GENERATED ALWAYS AS (
		CASE
			WHEN plan_week_number IS NULL THEN NULL
			ELSE CONCAT(
				LPAD(plan_week_number, 6, '0'),
				'|',
				scheduled_day_of_week,
				'|',
				COALESCE(DATE_FORMAT(planned_start_time, '%H:%i:%s.%f'), 'NONE'))
		END
	) STORED;

ALTER TABLE workout_days
	ADD CONSTRAINT uq_workout_days_plan_placement UNIQUE (training_plan_id, placement_key);

CREATE INDEX idx_workout_days_plan_week
	ON workout_days (training_plan_id, plan_week_number, scheduled_day_of_week);

ALTER TABLE workout_occurrences
	ADD COLUMN origin VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
	ADD COLUMN generation_key VARCHAR(200) NULL,
	ADD COLUMN original_scheduled_date DATE NULL,
	ADD COLUMN manually_rescheduled BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE workout_occurrences
	ADD CONSTRAINT uq_workout_occurrences_generation_key UNIQUE (generation_key);

CREATE INDEX idx_workout_occurrences_plan_origin
	ON workout_occurrences (training_plan_id, origin);
CREATE INDEX idx_workout_occurrences_day_origin
	ON workout_occurrences (workout_day_id, origin);
CREATE INDEX idx_workout_occurrences_athlete_date_status
	ON workout_occurrences (athlete_id, scheduled_date, status);
CREATE INDEX idx_workout_occurrences_origin
	ON workout_occurrences (origin);
CREATE INDEX idx_workout_occurrences_manually_rescheduled
	ON workout_occurrences (manually_rescheduled);
