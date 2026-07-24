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
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("4");
		assertThat(flyway.info().current().getDescription()).isEqualTo("create refresh sessions");
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

}
