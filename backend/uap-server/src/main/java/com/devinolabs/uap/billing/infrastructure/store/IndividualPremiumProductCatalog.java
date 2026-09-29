package com.devinolabs.uap.billing.infrastructure.store;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;

/**
 * Shared Individual Premium monthly/annual product-id allow-list for store billing properties.
 */
public final class IndividualPremiumProductCatalog {

	private IndividualPremiumProductCatalog() {
	}

	public record CatalogSlot(CommercialPlanKey planKey, BillingCadence cadence) {
	}

	public record PricedProduct(CommercialPlanKey planKey, BillingCadence cadence) {
	}

	public static Map<CatalogSlot, String> catalog(
			String individualPremiumMonthly,
			String individualPremiumAnnual,
			String productsPropertyPrefix,
			String enabledStoreLabel) {
		Map<CatalogSlot, String> catalog = new HashMap<>();
		put(catalog, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY,
				individualPremiumMonthly, productsPropertyPrefix + "individual-premium-monthly",
				enabledStoreLabel);
		put(catalog, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.ANNUAL,
				individualPremiumAnnual, productsPropertyPrefix + "individual-premium-annual",
				enabledStoreLabel);
		return Map.copyOf(catalog);
	}

	public static void requireDistinctProductIds(Map<CatalogSlot, String> catalog, String storeLabel) {
		Set<String> distinct = new HashSet<>(catalog.values());
		if (distinct.size() != 2) {
			throw new IllegalStateException(storeLabel + " Individual Premium product IDs must be distinct");
		}
	}

	public static PricedProduct requirePlanForProduct(
			Map<CatalogSlot, String> catalog,
			String productId,
			String storeLabel) {
		if (productId == null || productId.isBlank()) {
			throw new IllegalArgumentException(storeLabel + " product is not an allow-listed product");
		}
		String normalized = productId.trim();
		for (Map.Entry<CatalogSlot, String> entry : catalog.entrySet()) {
			if (entry.getValue().equals(normalized)) {
				return new PricedProduct(entry.getKey().planKey(), entry.getKey().cadence());
			}
		}
		throw new IllegalArgumentException(storeLabel + " product is not an allow-listed product");
	}

	public static String productId(
			Map<CatalogSlot, String> catalog,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			String storeLabel) {
		Objects.requireNonNull(planKey, "planKey must not be null");
		Objects.requireNonNull(cadence, "cadence must not be null");
		String value = catalog.get(new CatalogSlot(planKey, cadence));
		if (value == null) {
			throw new IllegalArgumentException(storeLabel + " catalog does not contain " + planKey);
		}
		return value;
	}

	private static void put(
			Map<CatalogSlot, String> catalog,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			String value,
			String propertyName,
			String enabledStoreLabel) {
		String normalized = StoreBillingPropertySupport.requireText(value, propertyName, enabledStoreLabel);
		catalog.put(new CatalogSlot(planKey, cadence), normalized);
	}

}
