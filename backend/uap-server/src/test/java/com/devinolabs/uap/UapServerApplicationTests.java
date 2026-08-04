package com.devinolabs.uap;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class UapServerApplicationTests {

	@Autowired
	private Flyway flyway;

	@Autowired
	private DataSource dataSource;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayStartsAndAppliesInitialMigration() {
		assertThat(flyway.info().current()).isNotNull();
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("26");
		assertThat(flyway.info().current().getDescription())
				.isEqualTo("add daily athlete state snapshots");
	}

	@Test
	void flywayAppliesIdentityAccountsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "accounts", new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "accounts", "email");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '2'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create identity accounts");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesEmailVerificationTokensMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "email_verification_tokens",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '3'")) {
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create email verification tokens");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesRefreshSessionsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "refresh_sessions",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '4'")) {
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create refresh sessions");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAthletesMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "athletes", new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "athletes", "account_id");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '5'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create athletes");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAthleteSportsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "athlete_sports",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "athlete_sports", "sport_identity");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '6'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create athlete sports");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAthleteGoalsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "athlete_goals",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "athlete_goals", "normalized_title");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '7'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create athlete goals");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAthleteMeasurementsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "athlete_measurements",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "athlete_measurements",
						"measurement_value");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '8'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create athlete measurements");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAssessmentsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "athlete_assessments",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "athlete_assessments",
						"normalized_title");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '9'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create assessments");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesAssessmentMeasurementsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "assessment_measurements",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "assessment_measurements",
						"snapshot_value");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '10'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create assessment measurements");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesTrainingPlansMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "training_plans",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "training_plans",
						"normalized_name");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '11'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create training plans");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutDaysMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "workout_days",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "workout_days",
						"normalized_title");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '12'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create workout days");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutExercisesMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "workout_exercises",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet columns = connection.getMetaData().getColumns(null, null, "workout_exercises",
						"normalized_exercise_name");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '13'")) {
			assertThat(columns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create workout exercises");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutOccurrencesMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "workout_occurrences",
						new String[] { "TABLE" })) {
			assertThat(tables.next()).isTrue();
		}

		try (Connection connection = dataSource.getConnection();
				ResultSet executionTables = connection.getMetaData().getTables(null, null,
						"workout_exercise_executions", new String[] { "TABLE" });
				ResultSet sessionTables = connection.getMetaData().getTables(null, null, "workout_sessions",
						new String[] { "TABLE" });
				ResultSet columns = connection.getMetaData().getColumns(null, null, "workout_occurrences",
						"active_scheduled_date");
				ResultSet executionColumns = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_exercise_name_snapshot");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '15'")) {
			assertThat(executionTables.next()).isTrue();
			assertThat(sessionTables.next()).isFalse();
			assertThat(columns.next()).isTrue();
			assertThat(executionColumns.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create workout occurrences and execution history");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesTrainingScheduleAndCalendarGenerationMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet scheduleStatus = connection.getMetaData().getColumns(null, null, "training_plans",
						"schedule_status");
				ResultSet recurrenceMode = connection.getMetaData().getColumns(null, null, "training_plans",
						"recurrence_mode");
				ResultSet planWeekNumber = connection.getMetaData().getColumns(null, null, "workout_days",
						"plan_week_number");
				ResultSet scheduledDayOfWeek = connection.getMetaData().getColumns(null, null, "workout_days",
						"scheduled_day_of_week");
				ResultSet legacyScheduledDay = connection.getMetaData().getColumns(null, null, "workout_days",
						"scheduled_day");
				ResultSet placementKey = connection.getMetaData().getColumns(null, null, "workout_days",
						"placement_key");
				ResultSet origin = connection.getMetaData().getColumns(null, null, "workout_occurrences", "origin");
				ResultSet generationKey = connection.getMetaData().getColumns(null, null, "workout_occurrences",
						"generation_key");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '16'")) {
			assertThat(scheduleStatus.next()).isTrue();
			assertThat(recurrenceMode.next()).isTrue();
			assertThat(planWeekNumber.next()).isTrue();
			assertThat(scheduledDayOfWeek.next()).isTrue();
			assertThat(legacyScheduledDay.next()).isFalse();
			assertThat(placementKey.next()).isTrue();
			assertThat(origin.next()).isTrue();
			assertThat(generationKey.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description"))
					.isEqualTo("add training schedule and calendar generation");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutExerciseSetsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet tables = connection.getMetaData().getTables(null, null, "workout_exercise_sets",
						new String[] { "TABLE" });
				ResultSet setNumber = connection.getMetaData().getColumns(null, null, "workout_exercise_sets",
						"set_number");
				ResultSet setStatus = connection.getMetaData().getColumns(null, null, "workout_exercise_sets",
						"status");
				ResultSet executionRpe = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "actual_rpe");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '17'")) {
			assertThat(tables.next()).isTrue();
			assertThat(setNumber.next()).isTrue();
			assertThat(setStatus.next()).isTrue();
			assertThat(executionRpe.next()).isTrue();
			assertThat(executionRpe.getString("TYPE_NAME")).containsIgnoringCase("decimal");
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("create workout exercise sets");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesTrainingPerformanceMetricsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet personalRecords = connection.getMetaData().getTables(null, null,
						"athlete_exercise_personal_records", new String[] { "TABLE" });
				ResultSet personalRecordHistory = connection.getMetaData().getTables(null, null,
						"athlete_exercise_personal_record_history", new String[] { "TABLE" });
				ResultSet performanceKey = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "exercise_performance_key");
				ResultSet qualifierKey = connection.getMetaData().getColumns(null, null,
						"athlete_exercise_personal_records", "record_qualifier_key");
				ResultSet supersededAt = connection.getMetaData().getColumns(null, null,
						"athlete_exercise_personal_record_history", "superseded_at");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '18'")) {
			assertThat(personalRecords.next()).isTrue();
			assertThat(personalRecordHistory.next()).isTrue();
			assertThat(performanceKey.next()).isTrue();
			assertThat(performanceKey.getInt("NULLABLE")).isZero();
			assertThat(qualifierKey.next()).isTrue();
			assertThat(supersededAt.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add training performance metrics");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesCanonicalExerciseDefinitionsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet definitions = connection.getMetaData().getTables(null, null, "exercise_definitions",
						new String[] { "TABLE" });
				ResultSet normalizedName = connection.getMetaData().getColumns(null, null, "exercise_definitions",
						"normalized_name");
				ResultSet prescriptionDefinition = connection.getMetaData().getColumns(null, null,
						"workout_exercises", "exercise_definition_id");
				ResultSet recordDefinition = connection.getMetaData().getColumns(null, null,
						"athlete_exercise_personal_records", "exercise_definition_id");
				ResultSet historyDefinition = connection.getMetaData().getColumns(null, null,
						"athlete_exercise_personal_record_history", "exercise_definition_id");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '19'")) {
			assertThat(definitions.next()).isTrue();
			assertThat(normalizedName.next()).isTrue();
			assertThat(prescriptionDefinition.next()).isTrue();
			assertThat(prescriptionDefinition.getInt("NULLABLE")).isZero();
			assertThat(recordDefinition.next()).isTrue();
			assertThat(historyDefinition.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add canonical exercise definitions");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutExerciseSubstitutionsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet substitutionHistory = connection.getMetaData().getTables(null, null,
						"workout_exercise_substitution_history", new String[] { "TABLE" });
				ResultSet prescribedDefinition = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "prescribed_exercise_definition_id");
				ResultSet prescribedName = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "prescribed_exercise_name_snapshot");
				ResultSet performedDefinition = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_exercise_definition_id");
				ResultSet performedName = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_exercise_name_snapshot");
				ResultSet substitutionReason = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "substitution_reason");
				ResultSet substitutedAt = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "substituted_at");
				ResultSet droppedDefinition = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "exercise_definition_id");
				ResultSet legacyName = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "exercise_name_snapshot");
				ResultSet reverted = connection.getMetaData().getColumns(null, null,
						"workout_exercise_substitution_history", "reverted");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '20'")) {
			assertThat(substitutionHistory.next()).isTrue();
			assertThat(prescribedDefinition.next()).isTrue();
			assertThat(prescribedDefinition.getInt("NULLABLE")).isZero();
			assertThat(prescribedName.next()).isTrue();
			assertThat(prescribedName.getInt("NULLABLE")).isZero();
			assertThat(performedDefinition.next()).isTrue();
			assertThat(performedDefinition.getInt("NULLABLE")).isZero();
			assertThat(performedName.next()).isTrue();
			assertThat(performedName.getInt("NULLABLE")).isZero();
			assertThat(substitutionReason.next()).isTrue();
			assertThat(substitutedAt.next()).isTrue();
			assertThat(droppedDefinition.next()).isFalse();
			assertThat(legacyName.next()).isFalse();
			assertThat(reverted.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add workout exercise substitutions");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesExerciseClassificationAndSubstitutionRelationshipsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet category = connection.getMetaData().getColumns(null, null, "exercise_definitions", "category");
				ResultSet relationships = connection.getMetaData().getTables(null, null,
						"exercise_substitution_relationships", new String[] { "TABLE" });
				ResultSet secondaryMuscles = connection.getMetaData().getTables(null, null,
						"exercise_definition_secondary_muscle_groups", new String[] { "TABLE" });
				ResultSet relationshipId = connection.getMetaData().getColumns(null, null,
						"workout_exercise_substitution_history", "substitution_relationship_id");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '21'")) {
			assertThat(category.next()).isTrue();
			assertThat(category.getInt("NULLABLE")).isZero();
			assertThat(relationships.next()).isTrue();
			assertThat(secondaryMuscles.next()).isTrue();
			assertThat(relationshipId.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description"))
					.isEqualTo("add exercise classification and substitution relationships");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesTrainingEnvironmentsAndOccurrenceContextMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet environments = connection.getMetaData().getTables(null, null, "training_environments",
						new String[] { "TABLE" });
				ResultSet equipment = connection.getMetaData().getTables(null, null,
						"training_environment_equipment", new String[] { "TABLE" });
				ResultSet plannedEnvironment = connection.getMetaData().getColumns(null, null, "workout_occurrences",
						"planned_training_environment_id");
				ResultSet planDefault = connection.getMetaData().getColumns(null, null, "training_plans",
						"default_training_environment_id");
				ResultSet historyEnvironment = connection.getMetaData().getColumns(null, null,
						"workout_exercise_substitution_history", "training_environment_id");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '22'")) {
			assertThat(environments.next()).isTrue();
			assertThat(equipment.next()).isTrue();
			assertThat(plannedEnvironment.next()).isTrue();
			assertThat(planDefault.next()).isTrue();
			assertThat(historyEnvironment.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description"))
					.isEqualTo("add training environments and occurrence context");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutAdaptationProposalsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet proposals = connection.getMetaData().getTables(null, null,
						"workout_adaptation_proposals", new String[] { "TABLE" });
				ResultSet items = connection.getMetaData().getTables(null, null,
						"workout_adaptation_proposal_items", new String[] { "TABLE" });
				ResultSet alternatives = connection.getMetaData().getTables(null, null,
						"workout_adaptation_proposal_item_alternatives", new String[] { "TABLE" });
				ResultSet historyProposal = connection.getMetaData().getColumns(null, null,
						"workout_exercise_substitution_history", "workout_adaptation_proposal_id");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '23'")) {
			assertThat(proposals.next()).isTrue();
			assertThat(items.next()).isTrue();
			assertThat(alternatives.next()).isTrue();
			assertThat(historyProposal.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add workout adaptation proposals");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesWorkoutSessionEffortAndTrainingLoadMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet sessionEfforts = connection.getMetaData().getTables(null, null,
						"workout_session_efforts", new String[] { "TABLE" });
				ResultSet revisions = connection.getMetaData().getTables(null, null,
						"workout_session_effort_revisions", new String[] { "TABLE" });
				ResultSet loadSummaries = connection.getMetaData().getTables(null, null,
						"workout_occurrence_load_summaries", new String[] { "TABLE" });
				ResultSet categorySummaries = connection.getMetaData().getTables(null, null,
						"workout_occurrence_load_category_summaries", new String[] { "TABLE" });
				ResultSet movementSummaries = connection.getMetaData().getTables(null, null,
						"workout_occurrence_load_movement_summaries", new String[] { "TABLE" });
				ResultSet categorySnapshot = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_exercise_category_snapshot");
				ResultSet patternSnapshot = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_primary_movement_pattern_snapshot");
				ResultSet impactSnapshot = connection.getMetaData().getColumns(null, null,
						"workout_exercise_executions", "performed_impact_level_snapshot");
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '24'")) {
			assertThat(sessionEfforts.next()).isTrue();
			assertThat(revisions.next()).isTrue();
			assertThat(loadSummaries.next()).isTrue();
			assertThat(categorySummaries.next()).isTrue();
			assertThat(movementSummaries.next()).isTrue();
			assertThat(categorySnapshot.next()).isTrue();
			assertThat(categorySnapshot.getInt("NULLABLE")).isZero();
			assertThat(patternSnapshot.next()).isTrue();
			assertThat(patternSnapshot.getInt("NULLABLE")).isZero();
			assertThat(impactSnapshot.next()).isTrue();
			assertThat(impactSnapshot.getInt("NULLABLE")).isZero();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description"))
					.isEqualTo("add workout session effort and training load");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesDailyRecoveryCheckInsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet checkIns = connection.getMetaData().getTables(null, null,
						"daily_recovery_check_ins", new String[] { "TABLE" });
				ResultSet discomfort = connection.getMetaData().getTables(null, null,
						"daily_recovery_check_in_discomfort", new String[] { "TABLE" });
				ResultSet revisions = connection.getMetaData().getTables(null, null,
						"daily_recovery_check_in_revisions", new String[] { "TABLE" });
				ResultSet revisionDiscomfort = connection.getMetaData().getTables(null, null,
						"daily_recovery_check_in_revision_discomfort", new String[] { "TABLE" });
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '25'")) {
			assertThat(checkIns.next()).isTrue();
			assertThat(discomfort.next()).isTrue();
			assertThat(revisions.next()).isTrue();
			assertThat(revisionDiscomfort.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add daily recovery check ins");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

	@Test
	void flywayAppliesDailyAthleteStateSnapshotsMigration() throws Exception {
		try (Connection connection = dataSource.getConnection();
				ResultSet snapshots = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_snapshots", new String[] { "TABLE" });
				ResultSet metrics = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_recovery_metrics", new String[] { "TABLE" });
				ResultSet discomfort = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_discomfort", new String[] { "TABLE" });
				ResultSet categories = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_category_summaries", new String[] { "TABLE" });
				ResultSet movements = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_movement_summaries", new String[] { "TABLE" });
				ResultSet scheduled = connection.getMetaData().getTables(null, null,
						"daily_athlete_state_scheduled_occurrences", new String[] { "TABLE" });
				ResultSet versions = connection.createStatement()
						.executeQuery("SELECT version, description, success FROM flyway_schema_history WHERE version = '26'")) {
			assertThat(snapshots.next()).isTrue();
			assertThat(metrics.next()).isTrue();
			assertThat(discomfort.next()).isTrue();
			assertThat(categories.next()).isTrue();
			assertThat(movements.next()).isTrue();
			assertThat(scheduled.next()).isTrue();
			assertThat(versions.next()).isTrue();
			assertThat(versions.getString("description")).isEqualTo("add daily athlete state snapshots");
			assertThat(versions.getBoolean("success")).isTrue();
		}
	}

}
