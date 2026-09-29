package com.devinolabs.uap.billing.infrastructure.google;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class GooglePlayBillingPropertiesTests {

	@Test
	void disabledConfigurationDoesNotRequireGooglePlayValues() {
		GooglePlayBillingProperties properties = new GooglePlayBillingProperties();

		properties.validateWhenEnabled();
	}

	@Test
	void enabledConfigurationRequiresPackageNameAndServiceAccount() {
		GooglePlayBillingProperties properties = validProperties();
		properties.setPackageName(" ");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("package-name");
	}

	@Test
	void enabledConfigurationRejectsProductionEnvironment() {
		GooglePlayBillingProperties properties = validProperties();
		properties.setEnvironment("Production");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Production");
	}

	@Test
	void enabledConfigurationRequiresDistinctProductIds() {
		GooglePlayBillingProperties properties = validProperties();
		properties.getProducts().setIndividualPremiumAnnual("premium.monthly");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("distinct");
	}

	@Test
	void enabledConfigurationRequiresServiceAccountPrivateKey() {
		GooglePlayBillingProperties properties = validProperties();
		properties.setServiceAccountJson("""
				{"type":"service_account","client_email":"play@test.iam.gserviceaccount.com","private_key":"not-a-pem"}
				""");

		assertThatThrownBy(properties::validateWhenEnabled)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("private_key");
	}

	@Test
	void allowListedProductResolvesIndividualPremiumCadence() {
		GooglePlayBillingProperties properties = validProperties();
		properties.validateWhenEnabled();

		assertThat(properties.requirePlanForProduct("premium.monthly").cadence().name())
				.isEqualTo("MONTHLY");
		assertThat(properties.requirePlanForProduct("premium.annual").planKey().name())
				.isEqualTo("INDIVIDUAL_PREMIUM");
		assertThat(properties.clientEmail()).isEqualTo("play@test.iam.gserviceaccount.com");
	}

	private static GooglePlayBillingProperties validProperties() {
		GooglePlayBillingProperties properties = new GooglePlayBillingProperties();
		properties.setEnabled(true);
		properties.setPackageName("com.devinolabs.athletereadiness");
		properties.setServiceAccountJson("""
				{"type":"service_account","project_id":"test",
				"client_email":"play@test.iam.gserviceaccount.com",
				"private_key":"-----BEGIN PRIVATE KEY-----\\nMIIEowIBAAKCAQEA0Z3VS5JJcds3xfn\\n-----END PRIVATE KEY-----\\n"}
				""");
		properties.setEnvironment("Sandbox");
		properties.getProducts().setIndividualPremiumMonthly("premium.monthly");
		properties.getProducts().setIndividualPremiumAnnual("premium.annual");
		return properties;
	}

}
