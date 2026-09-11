package com.devinolabs.uap.billing.domain;

import java.util.Objects;

/**
 * Fixed active-athlete band capacities (ADR-041). Usage counting/enforcement is Slice D.
 */
public enum OrganizationAthleteBand {

	BAND_25(CommercialPlanKey.ORG_BAND_25, 25),
	BAND_75(CommercialPlanKey.ORG_BAND_75, 75),
	BAND_250(CommercialPlanKey.ORG_BAND_250, 250);

	private final CommercialPlanKey planKey;
	private final int maxActiveAthletes;

	OrganizationAthleteBand(CommercialPlanKey planKey, int maxActiveAthletes) {
		this.planKey = planKey;
		this.maxActiveAthletes = maxActiveAthletes;
	}

	public CommercialPlanKey planKey() {
		return planKey;
	}

	public int maxActiveAthletes() {
		return maxActiveAthletes;
	}

	public static OrganizationAthleteBand forPlan(CommercialPlanKey planKey) {
		Objects.requireNonNull(planKey, "planKey must not be null");
		for (OrganizationAthleteBand band : values()) {
			if (band.planKey == planKey) {
				return band;
			}
		}
		throw new IllegalArgumentException("Not an organization band plan: " + planKey);
	}

}
