package com.devinolabs.uap.integrations.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@ConfigurationProperties(prefix = "uap.integrations")
public class IntegrationsProperties {

	/**
	 * Module presence flag. Default true — integrations boots without secrets.
	 * Per-provider flags remain fail-closed until explicitly enabled.
	 */
	private boolean enabled = true;

	/**
	 * Centrally owned initial backfill window (days). F2 evidence ingest rejects
	 * {@code observedAt} older than this window (ADR-051 / D4 = B).
	 */
	private int backfillDays = 30;

	private final ProviderToggle appleHealthkit = new ProviderToggle();
	private final ProviderToggle healthConnect = new ProviderToggle();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getBackfillDays() {
		return backfillDays;
	}

	public void setBackfillDays(int backfillDays) {
		if (backfillDays < 1) {
			throw new IllegalArgumentException("uap.integrations.backfill-days must be >= 1");
		}
		this.backfillDays = backfillDays;
	}

	public ProviderToggle getAppleHealthkit() {
		return appleHealthkit;
	}

	public ProviderToggle getHealthConnect() {
		return healthConnect;
	}

	public boolean isProviderEnabled(HealthProviderKey provider) {
		return switch (provider) {
			case APPLE_HEALTHKIT -> appleHealthkit.isEnabled();
			case HEALTH_CONNECT -> healthConnect.isEnabled();
		};
	}

	public static class ProviderToggle {

		private boolean enabled = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}
	}

}
