package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.mock.env.MockEnvironment;

class OrganizationCapacityEnforcementPropertiesTests {

	@Test
	void javaDefaultIsDisabled() {
		assertThat(new OrganizationCapacityEnforcementProperties().isEnabled()).isFalse();
	}

	@Test
	void absentBindingLeavesCapacityDisabled() {
		OrganizationCapacityEnforcementProperties properties = Binder.get(new MockEnvironment())
				.bind("uap.billing.organization-capacity-enforcement", OrganizationCapacityEnforcementProperties.class)
				.orElseGet(OrganizationCapacityEnforcementProperties::new);

		assertThat(properties.isEnabled()).isFalse();
	}

	@Test
	void explicitFalseIsDisabledIndependentlyOfStripeAndEntitlement() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.organization-capacity-enforcement.enabled", "false");
		environment.setProperty("uap.billing.stripe.enabled", "true");
		environment.setProperty("uap.billing.entitlement-enforcement.enabled", "true");

		assertThat(bind(environment).isEnabled()).isFalse();
	}

	@Test
	void explicitTrueIsEnabledIndependentlyOfStripeAndEntitlement() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.organization-capacity-enforcement.enabled", "true");
		environment.setProperty("uap.billing.stripe.enabled", "false");
		environment.setProperty("uap.billing.entitlement-enforcement.enabled", "false");

		assertThat(bind(environment).isEnabled()).isTrue();
	}

	@Test
	void stripeOrEntitlementEnabledDoesNotBindCapacity() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.stripe.enabled", "true");
		environment.setProperty("uap.billing.entitlement-enforcement.enabled", "true");

		assertThat(bind(environment).isEnabled()).isFalse();
	}

	private static OrganizationCapacityEnforcementProperties bind(MockEnvironment environment) {
		return Binder.get(environment)
				.bind("uap.billing.organization-capacity-enforcement", OrganizationCapacityEnforcementProperties.class)
				.orElseGet(OrganizationCapacityEnforcementProperties::new);
	}
}
