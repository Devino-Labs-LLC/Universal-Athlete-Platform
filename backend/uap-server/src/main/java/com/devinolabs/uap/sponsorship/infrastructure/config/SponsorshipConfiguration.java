package com.devinolabs.uap.sponsorship.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SponsorshipProperties.class)
class SponsorshipConfiguration {
}
