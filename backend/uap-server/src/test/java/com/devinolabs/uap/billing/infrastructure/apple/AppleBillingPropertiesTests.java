package com.devinolabs.uap.billing.infrastructure.apple;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppleBillingPropertiesTests {

	@Test
	void disabledConfigurationDoesNotRequireAppleValues() {
		AppleBillingProperties properties = new AppleBillingProperties();

		properties.validateWhenEnabled();
	}

	@Test
	void enabledConfigurationRequiresBundleAndApiCredentials() {
		AppleBillingProperties properties = validProperties();
		properties.setBundleId(" ");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("bundle-id");
	}

	@Test
	void enabledConfigurationRejectsProductionEnvironment() {
		AppleBillingProperties properties = validProperties();
		properties.setEnvironment("Production");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Production");
	}

	@Test
	void enabledConfigurationRequiresDistinctProductIds() {
		AppleBillingProperties properties = validProperties();
		properties.getProducts().setIndividualPremiumAnnual("premium.monthly");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("distinct");
	}

	@Test
	void enabledConfigurationRequiresPemPrivateKey() {
		AppleBillingProperties properties = validProperties();
		properties.setPrivateKeyPem("not-a-pem");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("private-key-pem");
	}

	@Test
	void allowListedProductResolvesIndividualPremiumCadence() {
		AppleBillingProperties properties = validProperties();
		properties.validateWhenEnabled();

		assertThat(properties.requirePlanForProduct("premium.monthly").cadence().name())
				.isEqualTo("MONTHLY");
		assertThat(properties.requirePlanForProduct("premium.annual").planKey().name())
				.isEqualTo("INDIVIDUAL_PREMIUM");
	}

	private static AppleBillingProperties validProperties() {
		AppleBillingProperties properties = new AppleBillingProperties();
		properties.setEnabled(true);
		properties.setBundleId("com.devinolabs.athletereadiness");
		properties.setKeyId("KEYID123");
		properties.setIssuerId("00000000-0000-0000-0000-000000000001");
		properties.setPrivateKeyPem("""
				-----BEGIN PRIVATE KEY-----
				MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgZ REPLACE_ME
				-----END PRIVATE KEY-----
				""".replace("Z REPLACE_ME", "TEST"));
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");
		return properties;
	}

}
