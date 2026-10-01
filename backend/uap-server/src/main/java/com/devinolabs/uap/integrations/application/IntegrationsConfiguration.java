package com.devinolabs.uap.integrations.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(IntegrationsProperties.class)
class IntegrationsConfiguration {
}
