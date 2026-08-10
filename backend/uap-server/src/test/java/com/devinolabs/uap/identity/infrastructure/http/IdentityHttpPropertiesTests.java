package com.devinolabs.uap.identity.infrastructure.http;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

class IdentityHttpPropertiesTests {

	@Test
	void validateAcceptsLocalDevelopmentDefaults() {
		IdentityHttpProperties properties = localDefaults();
		assertThatCode(properties::validate).doesNotThrowAnyException();
	}

	@Test
	void validateRejectsSameSiteNoneWithoutSecureCookie() {
		IdentityHttpProperties properties = localDefaults();
		properties.getCookies().setSameSite(IdentityHttpProperties.SameSite.NONE);
		properties.getCookies().setSecure(false);

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("same-site=NONE requires");
	}

	@Test
	void validateRejectsWildcardCorsWithCredentials() {
		IdentityHttpProperties properties = localDefaults();
		properties.getCors().setAllowedOrigins(List.of("*"));

		assertThatThrownBy(properties::validate)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("wildcard");
	}

	@Test
	void productionSafetyRejectsInsecureCookies() {
		IdentityHttpProperties properties = localDefaults();
		properties.getCookies().setSecure(false);
		properties.getCors().setAllowedOrigins(List.of("https://app.example.com"));

		assertThatThrownBy(properties::validateProductionSafety)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("UAP_COOKIE_SECURE");
	}

	@Test
	void productionSafetyRejectsLocalhostCorsOrigins() {
		IdentityHttpProperties properties = localDefaults();
		properties.getCookies().setSecure(true);
		properties.getCors().setAllowedOrigins(List.of("https://app.example.com", "http://localhost:3000"));

		assertThatThrownBy(properties::validateProductionSafety)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("localhost");
	}

	@Test
	void productionSafetyAcceptsHardenedProductionSettings() {
		IdentityHttpProperties properties = localDefaults();
		properties.getCookies().setSecure(true);
		properties.getCors().setAllowedOrigins(List.of("https://app.example.com"));

		assertThatCode(properties::validateProductionSafety).doesNotThrowAnyException();
	}

	private static IdentityHttpProperties localDefaults() {
		IdentityHttpProperties properties = new IdentityHttpProperties();
		properties.getCookies().setSecure(false);
		properties.getCookies().setSameSite(IdentityHttpProperties.SameSite.LAX);
		properties.getCors().setAllowedOrigins(List.of("http://localhost:3000"));
		properties.getCors().setAllowCredentials(true);
		properties.getRateLimit().setCapacity(30);
		properties.getRateLimit().setWindow(Duration.ofMinutes(1));
		return properties;
	}

}
