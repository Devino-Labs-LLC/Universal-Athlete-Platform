package com.devinolabs.uap;

import static org.assertj.core.api.Assertions.assertThat;

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

	@Test
	void contextLoads() {
	}

	@Test
	void flywayStartsAndAppliesInitialMigration() {
		assertThat(flyway.info().current()).isNotNull();
		assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
		assertThat(flyway.info().current().getDescription()).isEqualTo("initialize platform");
	}

}
