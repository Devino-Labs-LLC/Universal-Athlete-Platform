package com.devinolabs.uap.identity.infrastructure;

import java.time.Clock;
import java.time.Duration;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.LockoutPolicy;
import com.devinolabs.uap.identity.infrastructure.security.ConfigurableLockoutPolicy;
import com.devinolabs.uap.identity.infrastructure.security.HmacJwtAccessTokenIssuer;

@Configuration
@EnableConfigurationProperties(IdentityAuthProperties.class)
class IdentityConfiguration {

	@Bean
	@ConditionalOnMissingBean(Clock.class)
	Clock clock() {
		return Clock.systemUTC();
	}

	@Bean
	LockoutPolicy lockoutPolicy(IdentityAuthProperties properties) {
		return new ConfigurableLockoutPolicy(properties);
	}

	@Bean
	Duration refreshTokenTtl(IdentityAuthProperties properties) {
		return properties.getRefreshTokenTtl();
	}

	@Bean
	AccessTokenIssuer accessTokenIssuer(IdentityAuthProperties properties, Clock clock) {
		properties.validate();
		return new HmacJwtAccessTokenIssuer(properties, clock);
	}

}
