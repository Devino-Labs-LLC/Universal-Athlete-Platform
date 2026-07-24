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
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("8");
		assertThat(flyway.info().current().getDescription()).isEqualTo("create athlete measurements");
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

}
