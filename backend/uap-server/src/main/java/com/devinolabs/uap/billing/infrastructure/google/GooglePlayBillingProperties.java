package com.devinolabs.uap.billing.infrastructure.google;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.infrastructure.store.IndividualPremiumProductCatalog;
import com.devinolabs.uap.billing.infrastructure.store.StoreBillingPropertySupport;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@ConfigurationProperties(prefix = "uap.billing.google-play")
public class GooglePlayBillingProperties {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String ENABLED_LABEL = "Google Play";
	private static final String PRODUCTS_PREFIX = "uap.billing.google-play.products.";

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
		packageName = StoreBillingPropertySupport.requireText(
				packageName, "uap.billing.google-play.package-name", ENABLED_LABEL);
		serviceAccountJson = StoreBillingPropertySupport.requireText(
				serviceAccountJson, "uap.billing.google-play.service-account-json", ENABLED_LABEL);
		environment = StoreBillingPropertySupport.requireSandboxEnvironment(
				environment,
				"uap.billing.google-play.environment",
				ENABLED_LABEL,
				"uap.billing.google-play.environment Production is not authorized in V4 G3 (Sandbox only)");
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
			clientEmail = StoreBillingPropertySupport.requireText(
					text(root, "client_email"),
					"uap.billing.google-play.service-account-json.client_email",
					ENABLED_LABEL);
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
			IndividualPremiumProductCatalog.requireDistinctProductIds(catalog(), "Google Play");
		}

		private PricedProduct requirePlanForProduct(String productId) {
			IndividualPremiumProductCatalog.PricedProduct priced =
					IndividualPremiumProductCatalog.requirePlanForProduct(catalog(), productId, "Google Play");
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
