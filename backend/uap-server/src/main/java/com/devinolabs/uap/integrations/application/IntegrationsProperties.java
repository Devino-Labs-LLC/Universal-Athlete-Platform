package com.devinolabs.uap.integrations.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@ConfigurationProperties(prefix = "uap.integrations")
public class IntegrationsProperties {

	private boolean enabled = true;
	private int backfillDays = 30;

	private final ProviderToggle appleHealthkit = new ProviderToggle();
	private final ProviderToggle healthConnect = new ProviderToggle();
	private final ProviderToggle garmin = new ProviderToggle();
	private final ProviderToggle fitbit = new ProviderToggle();
	private final ProviderToggle whoop = new ProviderToggle();
	private final ProviderToggle oura = new ProviderToggle();
	private final ProviderToggle polar = new ProviderToggle();
	private final ProviderToggle bleDirect = new ProviderToggle();

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

	public ProviderToggle getGarmin() {
		return garmin;
	}

	public ProviderToggle getFitbit() {
		return fitbit;
	}

	public ProviderToggle getWhoop() {
		return whoop;
	}

	public ProviderToggle getOura() {
		return oura;
	}

	public ProviderToggle getPolar() {
		return polar;
	}

	public ProviderToggle getBleDirect() {
		return bleDirect;
	}

	public boolean isProviderEnabled(HealthProviderKey provider) {
		return switch (provider) {
			case APPLE_HEALTHKIT -> appleHealthkit.isEnabled();
			case HEALTH_CONNECT -> healthConnect.isEnabled();
			case GARMIN -> garmin.isEnabled();
			case FITBIT -> fitbit.isEnabled();
			case WHOOP -> whoop.isEnabled();
			case OURA -> oura.isEnabled();
			case POLAR -> polar.isEnabled();
			case BLE_DIRECT -> bleDirect.isEnabled();
		};
	}

	public static class ProviderToggle {

		private boolean enabled = false;
		private boolean mock = false;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isMock() {
			return mock;
		}

		public void setMock(boolean mock) {
			this.mock = mock;
		}
	}
}
