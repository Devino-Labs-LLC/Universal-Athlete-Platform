package com.devinolabs.uap.aicoach.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.json.JsonMapper;

import com.devinolabs.uap.aicoach.gateway.DisabledModelGateway;
import com.devinolabs.uap.aicoach.gateway.ModelGateway;
import com.devinolabs.uap.aicoach.gateway.ModelHttpTransport;
import com.devinolabs.uap.aicoach.provider.groq.GroqModelGateway;
import com.devinolabs.uap.aicoach.provider.groq.JdkModelHttpTransport;

@Configuration
@EnableConfigurationProperties(AiCoachProperties.class)
class AiCoachConfiguration {

	AiCoachConfiguration(AiCoachProperties properties) {
		properties.validate();
	}

	@Bean
	ModelHttpTransport modelHttpTransport() {
		return new JdkModelHttpTransport();
	}

	@Bean
	tools.jackson.databind.json.JsonMapper aiCoachJsonMapper() {
		return tools.jackson.databind.json.JsonMapper.builder().build();
	}

	@Bean
	ModelGateway modelGateway(
			AiCoachProperties properties,
			ModelHttpTransport transport,
			JsonMapper objectMapper) {
		if (!properties.isEnabled()) {
			return new DisabledModelGateway();
		}
		return new GroqModelGateway(properties, transport, objectMapper);
	}

	@Bean
	com.devinolabs.uap.aicoach.application.CoachRateLimiter coachRateLimiter(
			AiCoachProperties properties,
			java.time.Clock clock) {
		return new com.devinolabs.uap.aicoach.application.CoachRateLimiter(
				properties.getRateLimit().getCapacity(),
				properties.getRateLimit().getWindow(),
				clock);
	}

}
