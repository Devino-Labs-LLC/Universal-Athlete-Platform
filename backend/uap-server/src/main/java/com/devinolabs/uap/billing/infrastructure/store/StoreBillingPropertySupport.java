package com.devinolabs.uap.billing.infrastructure.store;

/**
 * Shared validation helpers for Apple App Store and Google Play billing properties.
 */
public final class StoreBillingPropertySupport {

	private StoreBillingPropertySupport() {
	}

	public static String requireText(String value, String propertyName, String enabledStoreLabel) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(
					propertyName + " must be configured when " + enabledStoreLabel + " billing is enabled");
		}
		return value.trim();
	}

	/**
	 * Accepts only Sandbox or Production, then fails closed on Production for the current V4 gate.
	 *
	 * @param productionGateMessage full IllegalStateException message when Production is selected
	 */
	public static String requireSandboxEnvironment(
			String environment,
			String propertyName,
			String enabledStoreLabel,
			String productionGateMessage) {
		String normalized = requireText(environment, propertyName, enabledStoreLabel);
		if (!"Sandbox".equals(normalized) && !"Production".equals(normalized)) {
			throw new IllegalStateException(propertyName + " must be Sandbox or Production");
		}
		if ("Production".equals(normalized)) {
			throw new IllegalStateException(productionGateMessage);
		}
		return normalized;
	}

}
