package com.devinolabs.uap.billing.domain;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.api.CommercialCapability;

/**
 * Provider-neutral plan → capability / band catalog.
 */
public final class CommercialCatalog {

	private static final Set<CommercialCapability> ORGANIZATION_CAPABILITIES = Collections.unmodifiableSet(
			EnumSet.of(
					CommercialCapability.ORG_TEAM_MANAGEMENT,
					CommercialCapability.ORG_COACH_COLLABORATION,
					CommercialCapability.ORG_COACH_ATHLETE_VIEW,
					CommercialCapability.ORG_TEAM_READINESS));

	private static final Set<CommercialCapability> INDIVIDUAL_CAPABILITIES = Collections.unmodifiableSet(
			EnumSet.of(CommercialCapability.INDIVIDUAL_PREMIUM));

	private CommercialCatalog() {
	}

	public static void validatePlanForSubject(CommercialPlanKey planKey, BillingSubjectType subjectType) {
		Objects.requireNonNull(planKey, "planKey must not be null");
		Objects.requireNonNull(subjectType, "subjectType must not be null");
		if (planKey.requiredSubjectType() != subjectType) {
			throw new IllegalArgumentException(
					"Plan " + planKey + " requires subject type " + planKey.requiredSubjectType()
							+ " but was " + subjectType);
		}
	}

	public static Set<CommercialCapability> capabilitiesFor(CommercialPlanKey planKey) {
		Objects.requireNonNull(planKey, "planKey must not be null");
		return switch (planKey) {
			case ORG_BAND_25, ORG_BAND_75, ORG_BAND_250 -> ORGANIZATION_CAPABILITIES;
			case INDIVIDUAL_PREMIUM -> INDIVIDUAL_CAPABILITIES;
		};
	}

	public static Optional<OrganizationAthleteBand> bandFor(CommercialPlanKey planKey) {
		Objects.requireNonNull(planKey, "planKey must not be null");
		if (!planKey.isOrganizationPlan()) {
			return Optional.empty();
		}
		return Optional.of(OrganizationAthleteBand.forPlan(planKey));
	}

}
