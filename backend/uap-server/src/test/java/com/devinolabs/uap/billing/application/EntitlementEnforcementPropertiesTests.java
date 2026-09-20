package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.boot.context.properties.bind.Binder;

class EntitlementEnforcementPropertiesTests {

	@Test
	void javaDefaultIsDisabled() {
		assertThat(new EntitlementEnforcementProperties().isEnabled()).isFalse();
	}

	@Test
	void absentBindingLeavesEnforcementDisabled() {
		EntitlementEnforcementProperties properties = Binder.get(new MockEnvironment())
				.bind("uap.billing.entitlement-enforcement", EntitlementEnforcementProperties.class)
				.orElseGet(EntitlementEnforcementProperties::new);

		assertThat(properties.isEnabled()).isFalse();
	}

	@Test
	void explicitFalseIsDisabled() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.entitlement-enforcement.enabled", "false");
		environment.setProperty("uap.billing.stripe.enabled", "true");

		assertThat(bind(environment).isEnabled()).isFalse();
	}

	@Test
	void explicitTrueIsEnabledIndependentlyOfStripe() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.entitlement-enforcement.enabled", "true");
		environment.setProperty("uap.billing.stripe.enabled", "false");

		assertThat(bind(environment).isEnabled()).isTrue();
	}

	@Test
	void stripeEnabledDoesNotBindEntitlementEnforcement() {
		MockEnvironment environment = new MockEnvironment();
		environment.setProperty("uap.billing.stripe.enabled", "true");

		assertThat(bind(environment).isEnabled()).isFalse();
	}

	private static EntitlementEnforcementProperties bind(MockEnvironment environment) {
		return Binder.get(environment)
				.bind("uap.billing.entitlement-enforcement", EntitlementEnforcementProperties.class)
				.orElseGet(EntitlementEnforcementProperties::new);
	}
}
