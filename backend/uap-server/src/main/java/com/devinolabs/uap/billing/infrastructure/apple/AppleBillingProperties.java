package com.devinolabs.uap.billing.infrastructure.apple;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

@ConfigurationProperties(prefix = "uap.billing.apple")
public class AppleBillingProperties {

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
		bundleId = requireText(bundleId, "uap.billing.apple.bundle-id");
		keyId = requireText(keyId, "uap.billing.apple.key-id");
		issuerId = requireText(issuerId, "uap.billing.apple.issuer-id");
		privateKeyPem = requireText(privateKeyPem, "uap.billing.apple.private-key-pem");
		environment = requireText(environment, "uap.billing.apple.environment");
		if (!"Sandbox".equals(environment) && !"Production".equals(environment)) {
			throw new IllegalStateException(
					"uap.billing.apple.environment must be Sandbox or Production");
		}
		if ("Production".equals(environment)) {
			throw new IllegalStateException(
					"uap.billing.apple.environment Production is not authorized in V4 G2 (Sandbox only)");
		}
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

	String productId(CommercialPlanKey planKey, BillingCadence cadence) {
		return products.productId(planKey, cadence);
	}

	public record PricedProduct(CommercialPlanKey planKey, BillingCadence cadence) {
	}

	private static String requireText(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " must be configured when Apple billing is enabled");
		}
		return value.trim();
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
			Set<String> distinct = new HashSet<>(catalog().values());
			if (distinct.size() != 2) {
				throw new IllegalStateException("Apple Individual Premium product IDs must be distinct");
			}
		}

		private String productId(CommercialPlanKey planKey, BillingCadence cadence) {
			Objects.requireNonNull(planKey, "planKey must not be null");
			Objects.requireNonNull(cadence, "cadence must not be null");
			String value = catalog().get(new CatalogSlot(planKey, cadence));
			if (value == null) {
				throw new IllegalArgumentException("Apple catalog does not contain " + planKey);
			}
			return value;
		}

		private PricedProduct requirePlanForProduct(String productId) {
			if (productId == null || productId.isBlank()) {
				throw new IllegalArgumentException("Apple product is not an allow-listed product");
			}
			String normalized = productId.trim();
			for (Map.Entry<CatalogSlot, String> entry : catalog().entrySet()) {
				if (entry.getValue().equals(normalized)) {
					return new PricedProduct(entry.getKey().planKey(), entry.getKey().cadence());
				}
			}
			throw new IllegalArgumentException("Apple product is not an allow-listed product");
		}

		private Map<CatalogSlot, String> catalog() {
			Map<CatalogSlot, String> catalog = new java.util.HashMap<>();
			put(catalog, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY,
					individualPremiumMonthly, "individual-premium-monthly");
			put(catalog, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.ANNUAL,
					individualPremiumAnnual, "individual-premium-annual");
			return Map.copyOf(catalog);
		}

		private static void put(
				Map<CatalogSlot, String> catalog,
				CommercialPlanKey planKey,
				BillingCadence cadence,
				String value,
				String propertySuffix) {
			String normalized = requireText(value, "uap.billing.apple.products." + propertySuffix);
			catalog.put(new CatalogSlot(planKey, cadence), normalized);
		}
	}

	private record CatalogSlot(CommercialPlanKey planKey, BillingCadence cadence) {
	}

}
