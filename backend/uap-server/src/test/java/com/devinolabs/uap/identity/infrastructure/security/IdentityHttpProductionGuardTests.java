package com.devinolabs.uap.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.devinolabs.uap.identity.infrastructure.http.IdentityHttpProperties;

class IdentityHttpProductionGuardTests {

	@Test
	void prodProfileRejectsInsecureCookieDefaults() {
		IdentityHttpProperties properties = localDefaults();
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("prod");

		assertThatThrownBy(() -> IdentitySecurityConfiguration.validateHttpProperties(properties, environment))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("UAP_COOKIE_SECURE");
	}

	@Test
	void nonProdProfileAllowsLocalCookieDefaults() {
		IdentityHttpProperties properties = localDefaults();
		MockEnvironment environment = new MockEnvironment();

		assertThatCode(() -> IdentitySecurityConfiguration.validateHttpProperties(properties, environment))
				.doesNotThrowAnyException();
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
