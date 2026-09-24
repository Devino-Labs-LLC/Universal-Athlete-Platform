package com.devinolabs.uap.billing.infrastructure.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

class StripeBillingPropertiesTests {

	@Test
	void disabledConfigurationDoesNotRequireStripeValues() {
		StripeBillingProperties properties = new StripeBillingProperties();

		properties.validateSandbox();
	}

	@Test
	void enabledConfigurationRequiresWebhookSigningSecret() {
		StripeBillingProperties properties = validProperties();
		properties.setWebhookSecret("not-a-stripe-webhook-secret");

		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("webhook");
	}

	@Test
	void enabledConfigurationRequiresATestModeKey() {
		StripeBillingProperties properties = validProperties();
		properties.setSecretKey("sk_live_not_allowed");

		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("test-mode");
	}

	@Test
	void enabledConfigurationRequiresEveryDistinctPrice() {
		StripeBillingProperties properties = validProperties();
		properties.getPrices().setOrgBand250Annual("price_25_monthly");

		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("distinct");
	}

	@Test
	void enabledConfigurationRequiresSafeReturnUrlsAndSessionPlaceholder() {
		StripeBillingProperties properties = validProperties();
		properties.setCancelUrl("http://billing.example.com/cancel");

		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("HTTPS or local HTTP");

		properties = validProperties();
		properties.setSuccessUrl("https://app.example.com/billing/success");
		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("CHECKOUT_SESSION_ID");
	}

	@Test
	void catalogMapsOnlyServerOwnedPlanAndCadencePairs() {
		StripeBillingProperties properties = validProperties();
		properties.validateSandbox();

		assertThat(properties.priceId(CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY))
				.isEqualTo("price_25_monthly");
		assertThat(properties.priceId(CommercialPlanKey.ORG_BAND_250, BillingCadence.ANNUAL))
				.isEqualTo("price_250_annual");
		assertThatThrownBy(() -> properties.priceId(
				CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void enabledConfigurationRequiresPortalConfigurationAndHttpsReturnUrl() {
		StripeBillingProperties properties = validProperties();
		properties.setPortalConfigurationId("price_not_a_portal_config");
		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("portal-configuration-id");

		properties = validProperties();
		properties.setPortalReturnUrl("http://billing.example.com/return");
		assertThatThrownBy(properties::validateSandbox)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("HTTPS or local HTTP");
	}

	@Test
	void reversePriceLookupAcceptsOnlyExactAllowListedIds() {
		StripeBillingProperties properties = validProperties();
		properties.validateSandbox();

		StripeBillingProperties.PricedPlan priced = properties.requirePlanForPrice("price_75_monthly");
		assertThat(priced.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(priced.cadence()).isEqualTo(BillingCadence.MONTHLY);
		assertThatThrownBy(() -> properties.requirePlanForPrice("price_75_monthly_extra"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageNotContaining("price_75_monthly_extra");
		assertThatThrownBy(() -> properties.requirePlanForPrice("price_unknown"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void productionProfileRejectsSandboxSlice() {
		StripeBillingProperties properties = validProperties();
		MockEnvironment environment = new MockEnvironment();
		environment.setActiveProfiles("production");

		assertThatThrownBy(() -> new StripeBillingConfiguration().stripeClient(properties, environment))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sandbox-only");
	}

	static StripeBillingProperties validProperties() {
		StripeBillingProperties properties = new StripeBillingProperties();
		properties.setEnabled(true);
		properties.setSecretKey("rk_test_placeholder_not_a_real_key");
		properties.setWebhookSecret("whsec_placeholder_not_a_real_secret");
		properties.setSuccessUrl("https://app.example.com/billing/success?session_id={CHECKOUT_SESSION_ID}");
		properties.setCancelUrl("https://app.example.com/billing/cancel");
		properties.setPortalConfigurationId("bpc_test_configuration");
		properties.setPortalReturnUrl("https://app.example.com/coach/billing");
		StripeBillingProperties.Prices prices = properties.getPrices();
		prices.setOrgBand25Monthly("price_25_monthly");
		prices.setOrgBand25Annual("price_25_annual");
		prices.setOrgBand75Monthly("price_75_monthly");
		prices.setOrgBand75Annual("price_75_annual");
		prices.setOrgBand250Monthly("price_250_monthly");
		prices.setOrgBand250Annual("price_250_annual");
		return properties;
	}

}
