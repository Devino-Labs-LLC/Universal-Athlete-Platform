package com.devinolabs.uap.billing.infrastructure.google;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ConfigurationProperties(prefix = "uap.billing.google-play")
public class GooglePlayBillingProperties {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private boolean enabled;
	private String packageName;
	private String serviceAccountJson;
	private String environment = "Sandbox";
	private Products products = new Products();

	private String clientEmail;
	private String privateKeyPem;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getPackageName() {
		return packageName;
	}

	public void setPackageName(String packageName) {
		this.packageName = packageName;
	}

	public String getServiceAccountJson() {
		return serviceAccountJson;
	}

	public void setServiceAccountJson(String serviceAccountJson) {
		this.serviceAccountJson = serviceAccountJson;
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

	String clientEmail() {
		return clientEmail;
	}

	String privateKeyPem() {
		return privateKeyPem;
	}

	public void validateWhenEnabled() {
		if (!enabled) {
			return;
		}
		packageName = requireText(packageName, "uap.billing.google-play.package-name");
		serviceAccountJson = requireText(serviceAccountJson, "uap.billing.google-play.service-account-json");
		environment = requireText(environment, "uap.billing.google-play.environment");
		if (!"Sandbox".equals(environment) && !"Production".equals(environment)) {
			throw new IllegalStateException(
					"uap.billing.google-play.environment must be Sandbox or Production");
		}
		if ("Production".equals(environment)) {
			throw new IllegalStateException(
					"uap.billing.google-play.environment Production is not authorized in V4 G3 (Sandbox only)");
		}
		parseServiceAccount(serviceAccountJson);
		if (products == null) {
			throw new IllegalStateException("uap.billing.google-play.products must be configured");
		}
		products.validate();
	}

	PricedProduct requirePlanForProduct(String productId) {
		return products.requirePlanForProduct(productId);
	}

	private void parseServiceAccount(String json) {
		try {
			JsonNode root = JSON.readTree(json);
			String type = text(root, "type");
			if (!"service_account".equals(type)) {
				throw new IllegalStateException(
						"uap.billing.google-play.service-account-json must be a Google service_account credential");
			}
			clientEmail = requireText(text(root, "client_email"),
					"uap.billing.google-play.service-account-json.client_email");
			String privateKey = text(root, "private_key");
			if (privateKey == null || !privateKey.contains("BEGIN PRIVATE KEY")) {
				throw new IllegalStateException(
						"uap.billing.google-play.service-account-json.private_key must be a PEM-encoded private key");
			}
			privateKeyPem = privateKey.replace("\\n", "\n");
		}
		catch (IllegalStateException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new IllegalStateException(
					"uap.billing.google-play.service-account-json could not be parsed", ex);
		}
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asString();
		return text == null || text.isBlank() ? null : text.trim();
	}

	public record PricedProduct(CommercialPlanKey planKey, BillingCadence cadence) {
	}

	private static String requireText(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " must be configured when Google Play billing is enabled");
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
				throw new IllegalStateException("Google Play Individual Premium product IDs must be distinct");
			}
		}

		private PricedProduct requirePlanForProduct(String productId) {
			if (productId == null || productId.isBlank()) {
				throw new IllegalArgumentException("Google Play product is not an allow-listed product");
			}
			String normalized = productId.trim();
			for (Map.Entry<CatalogSlot, String> entry : catalog().entrySet()) {
				if (entry.getValue().equals(normalized)) {
					return new PricedProduct(entry.getKey().planKey(), entry.getKey().cadence());
				}
			}
			throw new IllegalArgumentException("Google Play product is not an allow-listed product");
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
			String normalized = requireText(value, "uap.billing.google-play.products." + propertySuffix);
			catalog.put(new CatalogSlot(planKey, cadence), normalized);
		}
	}

	private record CatalogSlot(CommercialPlanKey planKey, BillingCadence cadence) {
	}

}
