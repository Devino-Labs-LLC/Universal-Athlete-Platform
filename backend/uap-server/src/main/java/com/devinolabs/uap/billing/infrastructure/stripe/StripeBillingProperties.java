package com.devinolabs.uap.billing.infrastructure.stripe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

@ConfigurationProperties(prefix = "uap.billing.stripe")
public class StripeBillingProperties {

	private boolean enabled;
	private String secretKey;
	private String webhookSecret;
	private String successUrl;
	private String cancelUrl;
	private Prices prices = new Prices();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSecretKey() {
		return secretKey;
	}

	public void setSecretKey(String secretKey) {
		this.secretKey = secretKey;
	}

	public String getWebhookSecret() {
		return webhookSecret;
	}

	public void setWebhookSecret(String webhookSecret) {
		this.webhookSecret = webhookSecret;
	}

	public String getSuccessUrl() {
		return successUrl;
	}

	public void setSuccessUrl(String successUrl) {
		this.successUrl = successUrl;
	}

	public String getCancelUrl() {
		return cancelUrl;
	}

	public void setCancelUrl(String cancelUrl) {
		this.cancelUrl = cancelUrl;
	}

	public Prices getPrices() {
		return prices;
	}

	public void setPrices(Prices prices) {
		this.prices = prices;
	}

	public void validateSandbox() {
		if (!enabled) {
			return;
		}
		String key = requireText(secretKey, "uap.billing.stripe.secret-key");
		if (!key.startsWith("rk_test_") && !key.startsWith("sk_test_")) {
			throw new IllegalStateException(
					"uap.billing.stripe.secret-key must be a Stripe test-mode restricted or secret key");
		}
		secretKey = key;
		webhookSecret = requireText(webhookSecret, "uap.billing.stripe.webhook-secret");
		if (!webhookSecret.startsWith("whsec_")) {
			throw new IllegalStateException("uap.billing.stripe.webhook-secret must be a Stripe webhook signing secret");
		}
		successUrl = requireText(successUrl, "uap.billing.stripe.success-url");
		cancelUrl = requireText(cancelUrl, "uap.billing.stripe.cancel-url");
		validateUrl(successUrl, "uap.billing.stripe.success-url");
		validateUrl(cancelUrl, "uap.billing.stripe.cancel-url");
		if (!successUrl.contains("{CHECKOUT_SESSION_ID}")) {
			throw new IllegalStateException(
					"uap.billing.stripe.success-url must include {CHECKOUT_SESSION_ID}");
		}
		if (prices == null) {
			throw new IllegalStateException("uap.billing.stripe.prices must be configured");
		}
		prices.validate();
	}

	String priceId(CommercialPlanKey planKey, BillingCadence cadence) {
		return prices.priceId(planKey, cadence);
	}

	void requireMatchingPrice(String providerPriceRef, CommercialPlanKey planKey, BillingCadence cadence) {
		if (!priceId(planKey, cadence).equals(providerPriceRef)) {
			throw new IllegalArgumentException("Provider Price does not match the requested plan and cadence");
		}
	}

	private static void validateUrl(String value, String propertyName) {
		String url = requireText(value, propertyName);
		try {
			URI uri = new URI(url.replace("{CHECKOUT_SESSION_ID}", "checkout_session"));
			if (!uri.isAbsolute() || uri.getHost() == null
					|| (!"https".equalsIgnoreCase(uri.getScheme()) && !isLocalHttp(uri))) {
				throw new IllegalStateException(propertyName + " must be HTTPS or local HTTP");
			}
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(propertyName + " must be a valid URI", ex);
		}
	}

	private static boolean isLocalHttp(URI uri) {
		return "http".equalsIgnoreCase(uri.getScheme())
				&& ("localhost".equalsIgnoreCase(uri.getHost()) || "127.0.0.1".equals(uri.getHost()));
	}

	private static String requireText(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " must be configured when Stripe billing is enabled");
		}
		return value.trim();
	}

	public static class Prices {

		private String orgBand25Monthly;
		private String orgBand25Annual;
		private String orgBand75Monthly;
		private String orgBand75Annual;
		private String orgBand250Monthly;
		private String orgBand250Annual;

		public String getOrgBand25Monthly() {
			return orgBand25Monthly;
		}

		public void setOrgBand25Monthly(String value) {
			this.orgBand25Monthly = value;
		}

		public String getOrgBand25Annual() {
			return orgBand25Annual;
		}

		public void setOrgBand25Annual(String value) {
			this.orgBand25Annual = value;
		}

		public String getOrgBand75Monthly() {
			return orgBand75Monthly;
		}

		public void setOrgBand75Monthly(String value) {
			this.orgBand75Monthly = value;
		}

		public String getOrgBand75Annual() {
			return orgBand75Annual;
		}

		public void setOrgBand75Annual(String value) {
			this.orgBand75Annual = value;
		}

		public String getOrgBand250Monthly() {
			return orgBand250Monthly;
		}

		public void setOrgBand250Monthly(String value) {
			this.orgBand250Monthly = value;
		}

		public String getOrgBand250Annual() {
			return orgBand250Annual;
		}

		public void setOrgBand250Annual(String value) {
			this.orgBand250Annual = value;
		}

		private void validate() {
			Set<String> distinct = new HashSet<>(catalog().values());
			if (distinct.size() != 6) {
				throw new IllegalStateException("All six Stripe Organization Price IDs must be distinct");
			}
		}

		private String priceId(CommercialPlanKey planKey, BillingCadence cadence) {
			Objects.requireNonNull(planKey, "planKey must not be null");
			Objects.requireNonNull(cadence, "cadence must not be null");
			String value = catalog().get(new CatalogSlot(planKey, cadence));
			if (value == null) {
				throw new IllegalArgumentException("Stripe Organization catalog does not contain " + planKey);
			}
			return value;
		}

		private Map<CatalogSlot, String> catalog() {
			Map<CatalogSlot, String> catalog = new java.util.HashMap<>();
			put(catalog, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY,
					orgBand25Monthly, "org-band-25-monthly");
			put(catalog, CommercialPlanKey.ORG_BAND_25, BillingCadence.ANNUAL,
					orgBand25Annual, "org-band-25-annual");
			put(catalog, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY,
					orgBand75Monthly, "org-band-75-monthly");
			put(catalog, CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL,
					orgBand75Annual, "org-band-75-annual");
			put(catalog, CommercialPlanKey.ORG_BAND_250, BillingCadence.MONTHLY,
					orgBand250Monthly, "org-band-250-monthly");
			put(catalog, CommercialPlanKey.ORG_BAND_250, BillingCadence.ANNUAL,
					orgBand250Annual, "org-band-250-annual");
			return Map.copyOf(catalog);
		}

		private static void put(
				Map<CatalogSlot, String> catalog,
				CommercialPlanKey planKey,
				BillingCadence cadence,
				String value,
				String propertySuffix) {
			String normalized = requireText(value, "uap.billing.stripe.prices." + propertySuffix);
			if (!normalized.startsWith("price_")) {
				throw new IllegalStateException(
						"uap.billing.stripe.prices." + propertySuffix + " must be a Stripe Price ID");
			}
			catalog.put(new CatalogSlot(planKey, cadence), normalized);
		}
	}

	private record CatalogSlot(CommercialPlanKey planKey, BillingCadence cadence) {
	}

}
