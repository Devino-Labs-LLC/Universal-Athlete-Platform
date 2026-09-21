package com.devinolabs.uap.entitlements;

/**
 * Provider-neutral Organization active-athlete band. Numeric maxima only.
 * Does not expose billing plan keys or provider identity.
 */
public enum OrganizationAthleteBand {

	BAND_25(25),
	BAND_75(75),
	BAND_250(250);

	private final int maxActiveAthletes;

	OrganizationAthleteBand(int maxActiveAthletes) {
		this.maxActiveAthletes = maxActiveAthletes;
	}

	public int maxActiveAthletes() {
		return maxActiveAthletes;
	}

}
