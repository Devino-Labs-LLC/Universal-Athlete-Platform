package com.devinolabs.uap.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.api.CommercialCapability;

class SubscriptionDomainTests {

	private static final Instant T0 = Instant.parse("2026-09-10T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);

	@Test
	void pendingIsNotEntitled() {
		Subscription subscription = pendingOrg();
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isFalse();
		assertThat(subscription.effectiveCapabilitiesAt(T0)).isEmpty();
	}

	@Test
	void organizationTrialEntitlesThroughTrialWindowOnly() {
		Subscription subscription = pendingOrg();
		subscription.beginOrganizationTrial(CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(subscription.trialEndsAt()).isEqualTo(Instant.parse("2026-09-24T12:00:00Z"));
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isTrue();
		assertThat(subscription.grantsCapabilityAt(CommercialCapability.ORG_TEAM_READINESS, T0)).isTrue();

		Instant afterTrial = Instant.parse("2026-09-24T12:00:00Z");
		assertThat(subscription.isCommerciallyEntitledAt(afterTrial)).isFalse();
	}

	@Test
	void individualPremiumCannotBeginOrganizationTrial() {
		Subscription subscription = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK);

		assertThatThrownBy(() -> subscription.beginOrganizationTrial(CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Organization trial");
	}

	@Test
	void individualPlanRejectedForOrganizationSubject() {
		assertThatThrownBy(() -> Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.organization(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("INDIVIDUAL_PREMIUM");
	}

	@Test
	void organizationPlanRejectedForAccountSubject() {
		assertThatThrownBy(() -> Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.ORG_BAND_25,
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("ORG_BAND_25");
	}

	@Test
	void activateFromPendingGrantsCapabilities() {
		Subscription subscription = pendingOrg();
		Instant periodEnd = Instant.parse("2026-10-10T12:00:00Z");
		subscription.activate(periodEnd, CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isTrue();
		assertThat(subscription.effectiveCapabilitiesAt(T0))
				.containsExactlyInAnyOrder(
						CommercialCapability.ORG_TEAM_MANAGEMENT,
						CommercialCapability.ORG_COACH_COLLABORATION,
						CommercialCapability.ORG_COACH_ATHLETE_VIEW,
						CommercialCapability.ORG_TEAM_READINESS);
	}

	@Test
	void pastDueIsNeverIndefinitelyEntitled() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.markPastDue(CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isFalse();
	}

	@Test
	void gracePeriodEntitlesUntilExplicitDeadline() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.enterGracePeriod(CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(subscription.graceEndsAt()).isEqualTo(Instant.parse("2026-09-17T12:00:00Z"));
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isTrue();
		assertThat(subscription.isCommerciallyEntitledAt(Instant.parse("2026-09-17T12:00:00Z"))).isFalse();
	}

	@Test
	void cancelAtPeriodEndEntitlesUntilPaidThrough() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		Instant paidThrough = Instant.parse("2026-10-10T12:00:00Z");
		subscription.scheduleCancelAtPeriodEnd(paidThrough, CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isTrue();
		assertThat(subscription.isCommerciallyEntitledAt(paidThrough)).isFalse();
	}

	@Test
	void expiredIsNotEntitled() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.expire(CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isFalse();
	}

	@Test
	void illegalTransitionFromExpiredIsRejected() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.expire(CLOCK);

		assertThatThrownBy(() -> subscription.reactivate(CLOCK))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void pendingToActivePathWorksForIndividualPremium() {
		Subscription subscription = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.randomUUID()),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK);
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);

		assertThat(subscription.grantsCapabilityAt(CommercialCapability.INDIVIDUAL_PREMIUM, T0)).isTrue();
		assertThat(subscription.grantsCapabilityAt(CommercialCapability.ORG_TEAM_MANAGEMENT, T0)).isFalse();
	}

	@Test
	void providerIdentityDoesNotChangeCapabilitySemantics() {
		UUID accountId = UUID.randomUUID();
		Subscription stripe = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK);
		Subscription apple = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK);
		stripe.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		apple.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);

		assertThat(stripe.effectiveCapabilitiesAt(T0)).isEqualTo(apple.effectiveCapabilitiesAt(T0));
	}

	@Test
	void organizationBandsAreDeterministic() {
		assertThat(OrganizationAthleteBand.BAND_25.maxActiveAthletes()).isEqualTo(25);
		assertThat(OrganizationAthleteBand.BAND_75.maxActiveAthletes()).isEqualTo(75);
		assertThat(OrganizationAthleteBand.BAND_250.maxActiveAthletes()).isEqualTo(250);
		assertThat(OrganizationAthleteBand.forPlan(CommercialPlanKey.ORG_BAND_75))
				.isEqualTo(OrganizationAthleteBand.BAND_75);
	}

	@Test
	void trialThenActiveThenCancelAtPeriodEndThenExpire() {
		Subscription subscription = pendingOrg();
		subscription.beginOrganizationTrial(CLOCK);
		subscription.convertTrialToActive(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.scheduleCancelAtPeriodEnd(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		subscription.expire(CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isFalse();
	}

	@Test
	void subjectTypeHelpers() {
		UUID id = UUID.randomUUID();
		assertThat(BillingSubject.account(id).type()).isEqualTo(BillingSubjectType.ACCOUNT);
		assertThat(BillingSubject.organization(id).type()).isEqualTo(BillingSubjectType.ORGANIZATION);
	}

	private static Subscription pendingOrg() {
		return Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.organization(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.ORG_BAND_25,
				CLOCK);
	}

}
