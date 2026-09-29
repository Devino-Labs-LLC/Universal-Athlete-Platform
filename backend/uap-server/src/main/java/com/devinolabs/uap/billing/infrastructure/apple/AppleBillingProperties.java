package com.devinolabs.uap.billing.infrastructure.apple;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.infrastructure.store.IndividualPremiumProductCatalog;
import com.devinolabs.uap.billing.infrastructure.store.StoreBillingPropertySupport;

@ConfigurationProperties(prefix = "uap.billing.apple")
public class AppleBillingProperties {

	private static final String ENABLED_LABEL = "Apple";
	private static final String PRODUCTS_PREFIX = "uap.billing.apple.products.";

	private boolean enabled;
	private String bundleId;
	private String keyId;
	private String issuerId;
	private String privateKeyPem;
	private String environment = "Sandbox";
	private Products products = new Products();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBundleId() {
		return bundleId;
	}

	public void setBundleId(String bundleId) {
		this.bundleId = bundleId;
	}

	public String getKeyId() {
		return keyId;
	}

	public void setKeyId(String keyId) {
		this.keyId = keyId;
	}

	public String getIssuerId() {
		return issuerId;
	}

	public void setIssuerId(String issuerId) {
		this.issuerId = issuerId;
	}

	public String getPrivateKeyPem() {
		return privateKeyPem;
	}

	public void setPrivateKeyPem(String privateKeyPem) {
		this.privateKeyPem = privateKeyPem;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public Products getProducts() {
		return products;
	}

	public void setProducts(Products products) {
		this.products = products;
	}

	public void validateWhenEnabled() {
		if (!enabled) {
			return;
		}
		bundleId = StoreBillingPropertySupport.requireText(bundleId, "uap.billing.apple.bundle-id", ENABLED_LABEL);
		keyId = StoreBillingPropertySupport.requireText(keyId, "uap.billing.apple.key-id", ENABLED_LABEL);
		issuerId = StoreBillingPropertySupport.requireText(issuerId, "uap.billing.apple.issuer-id", ENABLED_LABEL);
		privateKeyPem = StoreBillingPropertySupport.requireText(
				privateKeyPem, "uap.billing.apple.private-key-pem", ENABLED_LABEL);
		environment = StoreBillingPropertySupport.requireSandboxEnvironment(
				environment,
				"uap.billing.apple.environment",
				ENABLED_LABEL,
				"uap.billing.apple.environment Production is not authorized in V4 G2 (Sandbox only)");
		if (!privateKeyPem.contains("BEGIN PRIVATE KEY")) {
			throw new IllegalStateException(
					"uap.billing.apple.private-key-pem must be a PEM-encoded App Store Connect API private key");
		}
		if (products == null) {
			throw new IllegalStateException("uap.billing.apple.products must be configured");
		}
		products.validate();
	}

	PricedProduct requirePlanForProduct(String productId) {
		return products.requirePlanForProduct(productId);
	}

	public record PricedProduct(CommercialPlanKey planKey, BillingCadence cadence) {
	}

	public static class Products {

		private String individualPremiumMonthly;
		private String individualPremiumAnnual;

		public String getIndividualPremiumMonthly() {
			return individualPremiumMonthly;
		}

		public void setIndividualPremiumMonthly(String value) {
			this.individualPremiumMonthly = value;
		}

		public String getIndividualPremiumAnnual() {
			return individualPremiumAnnual;
		}

		public void setIndividualPremiumAnnual(String value) {
			this.individualPremiumAnnual = value;
		}

		private void validate() {
			IndividualPremiumProductCatalog.requireDistinctProductIds(catalog(), "Apple");
		}

		private PricedProduct requirePlanForProduct(String productId) {
			IndividualPremiumProductCatalog.PricedProduct priced =
					IndividualPremiumProductCatalog.requirePlanForProduct(catalog(), productId, "Apple");
			return new PricedProduct(priced.planKey(), priced.cadence());
		}

		private Map<IndividualPremiumProductCatalog.CatalogSlot, String> catalog() {
			return IndividualPremiumProductCatalog.catalog(
					individualPremiumMonthly,
					individualPremiumAnnual,
					PRODUCTS_PREFIX,
					ENABLED_LABEL);
		}
	}

}
