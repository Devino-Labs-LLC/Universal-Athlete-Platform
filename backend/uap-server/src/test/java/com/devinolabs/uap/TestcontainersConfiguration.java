package com.devinolabs.uap;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.devinolabs.uap.training.application.CreateAthleteExerciseDefinitionUseCase;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	MySQLContainer mysqlContainer() {
		return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
	}

	@Bean
	ExerciseDefinitionFixtures exerciseDefinitionFixtures(
			CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase) {
		return new ExerciseDefinitionFixtures(createAthleteExerciseDefinitionUseCase);
	}

}
