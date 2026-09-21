package com.devinolabs.uap.entitlements;

import java.util.Objects;

/**
 * Provider-neutral commercial capacity for an Organization.
 *
 * <p>Absence of an effective band is not a zero-seat band.
 */
public sealed interface OrganizationCommercialCapacity
		permits OrganizationCommercialCapacity.NoEffectiveBand,
				OrganizationCommercialCapacity.EffectiveBand,
				OrganizationCommercialCapacity.Ambiguous {

	record NoEffectiveBand() implements OrganizationCommercialCapacity {
	}

	record EffectiveBand(OrganizationAthleteBand band) implements OrganizationCommercialCapacity {

		public EffectiveBand {
			Objects.requireNonNull(band, "band must not be null");
		}

		public int maxActiveAthletes() {
			return band.maxActiveAthletes();
		}
	}

	record Ambiguous() implements OrganizationCommercialCapacity {
	}

}
