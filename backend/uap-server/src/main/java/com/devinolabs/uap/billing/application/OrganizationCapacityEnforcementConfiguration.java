package com.devinolabs.uap.billing.application;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(OrganizationCapacityEnforcementProperties.class)
class OrganizationCapacityEnforcementConfiguration {
}
