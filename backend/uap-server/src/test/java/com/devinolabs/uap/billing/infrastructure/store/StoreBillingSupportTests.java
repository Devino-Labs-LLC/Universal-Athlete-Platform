package com.devinolabs.uap.billing.infrastructure.store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

class IndividualPremiumProductCatalogTests {

	@Test
	void catalogResolvesDistinctAllowListedProducts() {
		Map<IndividualPremiumProductCatalog.CatalogSlot, String> catalog =
				IndividualPremiumProductCatalog.catalog(
						"premium.monthly",
						"premium.annual",
						"uap.billing.test.products.",
						"Test");

		IndividualPremiumProductCatalog.requireDistinctProductIds(catalog, "Test");
		assertThat(IndividualPremiumProductCatalog.requirePlanForProduct(catalog, "premium.monthly", "Test"))
				.isEqualTo(new IndividualPremiumProductCatalog.PricedProduct(
						CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY));
		assertThat(IndividualPremiumProductCatalog.productId(
				catalog, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.ANNUAL, "Test"))
				.isEqualTo("premium.annual");
	}

	@Test
	void catalogRejectsBlankDuplicateAndUnknownProducts() {
		assertThatThrownBy(() -> IndividualPremiumProductCatalog.catalog(
						" ", "premium.annual", "uap.billing.test.products.", "Test"))
				.isInstanceOf(IllegalStateException.class);

		Map<IndividualPremiumProductCatalog.CatalogSlot, String> duplicates =
				IndividualPremiumProductCatalog.catalog(
						"same", "same", "uap.billing.test.products.", "Test");
		assertThatThrownBy(() -> IndividualPremiumProductCatalog.requireDistinctProductIds(duplicates, "Test"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("distinct");

		Map<IndividualPremiumProductCatalog.CatalogSlot, String> catalog =
				IndividualPremiumProductCatalog.catalog(
						"premium.monthly", "premium.annual", "uap.billing.test.products.", "Test");
		assertThatThrownBy(() -> IndividualPremiumProductCatalog.requirePlanForProduct(catalog, " ", "Test"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> IndividualPremiumProductCatalog.requirePlanForProduct(
						catalog, "unknown", "Test"))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> IndividualPremiumProductCatalog.productId(
						catalog, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY, "Test"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("catalog does not contain");
	}

}

class StoreBillingPropertySupportTests {

	@Test
	void requireTextAndSandboxEnvironmentFailClosed() {
		assertThat(StoreBillingPropertySupport.requireText(" value ", "prop", "Apple")).isEqualTo("value");
		assertThatThrownBy(() -> StoreBillingPropertySupport.requireText(" ", "prop", "Apple"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Apple");

		assertThat(StoreBillingPropertySupport.requireSandboxEnvironment(
				"Sandbox", "env", "Apple", "production blocked"))
				.isEqualTo("Sandbox");
		assertThatThrownBy(() -> StoreBillingPropertySupport.requireSandboxEnvironment(
						"Staging", "env", "Apple", "production blocked"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Sandbox or Production");
		assertThatThrownBy(() -> StoreBillingPropertySupport.requireSandboxEnvironment(
						"Production", "env", "Apple", "production blocked"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("production blocked");
	}

}
