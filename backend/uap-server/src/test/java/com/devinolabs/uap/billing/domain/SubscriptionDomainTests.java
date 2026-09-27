package com.devinolabs.uap.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;

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
		subscription.recordExceptionalPaymentAttention(attention(subscription, ProviderCollectionState.UNPAID, T0.plusSeconds(1)), CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PAST_DUE);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isFalse();
	}

	@Test
	void gracePeriodEntitlesUntilExplicitDeadline() {
		Subscription subscription = pendingOrg();
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), CLOCK);
		Instant failure = T0;
		subscription.establishGrace(attention(subscription, ProviderCollectionState.PAST_DUE, T0.plusSeconds(1)), failure, CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(subscription.graceEndsAt()).isEqualTo(Instant.parse("2026-09-17T12:00:00Z"));
		assertThat(subscription.isCommerciallyEntitledAt(subscription.graceEndsAt().minusNanos(1))).isTrue();
		assertThat(subscription.isCommerciallyEntitledAt(subscription.graceEndsAt())).isFalse();
		assertThat(subscription.isCommerciallyEntitledAt(subscription.graceEndsAt().plusNanos(1))).isFalse();
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

	@Test
	void organizationCheckoutKeepsCadenceAndUsesProviderSnapshotTrialClock() {
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(UUID.randomUUID()),
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL,
				CLOCK);
		Instant providerAsOf = T0.plusSeconds(10);
		Instant trialEnd = T0.plusSeconds(14 * 24 * 60 * 60);

		boolean changed = subscription.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_trial",
						"sub_trial",
						ProviderCommercialStatus.TRIALING,
						false,
						trialEnd,
						T0.plusSeconds(30 * 24 * 60 * 60),
						CommercialPlanKey.ORG_BAND_75,
						BillingCadence.ANNUAL,
						providerAsOf),
				CLOCK);

		assertThat(changed).isTrue();
		assertThat(subscription.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(subscription.trialEndsAt()).isEqualTo(trialEnd);
		assertThat(subscription.providerStateAsOf()).isEqualTo(providerAsOf);
		assertThat(subscription.isCommerciallyEntitledAt(T0)).isTrue();
	}

	@Test
	void authoritativePriceReplacesPlanAndStaleSnapshotDoesNotRollItBack() {
		Subscription subscription = pendingCheckout();
		Instant trialEnd = T0.plusSeconds(14 * 24 * 60 * 60);
		Instant firstAsOf = T0.plusSeconds(10);
		assertThat(subscription.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_active",
						"sub_active",
						ProviderCommercialStatus.TRIALING,
						false,
						trialEnd,
						T0.plusSeconds(30 * 24 * 60 * 60),
						CommercialPlanKey.ORG_BAND_75,
						BillingCadence.ANNUAL,
						firstAsOf),
				CLOCK)).isTrue();
		assertThat(subscription.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(subscription.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
		assertThat(subscription.trialEndsAt()).isEqualTo(trialEnd);

		assertThat(subscription.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_active",
						"sub_active",
						ProviderCommercialStatus.TRIALING,
						false,
						trialEnd,
						T0.plusSeconds(30 * 24 * 60 * 60),
						CommercialPlanKey.ORG_BAND_25,
						BillingCadence.MONTHLY,
						firstAsOf),
				CLOCK)).isFalse();
		assertThat(subscription.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(subscription.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
		assertThat(subscription.trialEndsAt()).isEqualTo(trialEnd);
	}

	@Test
	void staleAndEqualProviderSnapshotsAreIgnored() {
		Subscription subscription = pendingCheckout();
		ProviderSubscriptionSnapshot current = activeSnapshot(T0.plusSeconds(20));
		assertThat(subscription.synchronizeProviderSnapshot(current, CLOCK)).isTrue();

		ProviderSubscriptionSnapshot equal = activeSnapshot(T0.plusSeconds(20));
		ProviderSubscriptionSnapshot stale = activeSnapshot(T0.plusSeconds(19));
		assertThat(subscription.synchronizeProviderSnapshot(equal, CLOCK)).isFalse();
		assertThat(subscription.synchronizeProviderSnapshot(stale, CLOCK)).isFalse();
	}

	@Test
	void providerReferenceMismatchAndUnknownStatusFailClosed() {
		Subscription subscription = pendingCheckout();
		subscription.synchronizeProviderSnapshot(activeSnapshot(T0.plusSeconds(20)), CLOCK);

		assertThatThrownBy(() -> subscription.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_foreign",
						"sub_active",
						ProviderCommercialStatus.ACTIVE,
						false,
						null,
						T0.plusSeconds(1_000),
						CommercialPlanKey.ORG_BAND_25,
						BillingCadence.MONTHLY,
						T0.plusSeconds(21)),
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("customer");

		Subscription unknown = pendingCheckout();
		assertThatThrownBy(() -> unknown.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_unknown",
						"sub_unknown",
						ProviderCommercialStatus.UNKNOWN,
						false,
						null,
						null,
						CommercialPlanKey.ORG_BAND_25,
						BillingCadence.MONTHLY,
						T0.plusSeconds(30)),
				CLOCK))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Unknown provider status");
		assertThat(unknown.isCommerciallyEntitledAt(T0)).isFalse();
	}

	@Test
	void providerSnapshotRequiresSubscriptionReferenceAndStateTimestamp() {
		assertThatThrownBy(() -> new ProviderSubscriptionSnapshot(
				"cus_test", " ", ProviderCommercialStatus.PENDING, false, null, null,
				CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY, T0))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new ProviderSubscriptionSnapshot(
				"cus_test", "sub_test", ProviderCommercialStatus.PENDING, false, null, null,
				CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY, null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void trialConversionFailureEntersGraceWithoutMovingTrialEnd() {
		Subscription subscription = pendingOrg();
		subscription.beginOrganizationTrial(CLOCK);
		Instant trialEnd = subscription.trialEndsAt();
		Instant failure = T0.plusSeconds(30);

		subscription.establishGrace(
				attention(subscription, ProviderCollectionState.PAST_DUE, failure),
				failure,
				CLOCK);

		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(subscription.trialEndsAt()).isEqualTo(trialEnd);
		assertThat(subscription.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(failure));
	}

	@Test
	void paymentAttentionOnTrialPreservesTrialEndWithoutStartingGrace() {
		Subscription subscription = pendingOrg();
		subscription.beginOrganizationTrial(CLOCK);
		subscription.attachProviderReferences("cus_active", "sub_active", CLOCK);
		Instant trialEnd = subscription.trialEndsAt();
		ProviderSubscriptionSnapshot attention = new ProviderSubscriptionSnapshot(
				"cus_active",
				"sub_active",
				ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
				false,
				null,
				T0.plusSeconds(30 * 24 * 60 * 60),
				subscription.planKey(),
				BillingCadence.MONTHLY,
				T0.plusSeconds(1),
				ProviderCollectionState.PAST_DUE);

		assertThat(subscription.synchronizeProviderSnapshot(attention, CLOCK)).isTrue();
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(subscription.trialEndsAt()).isEqualTo(trialEnd);
		assertThat(subscription.graceEndsAt()).isNull();
	}

	@Test
	void laterFailureDoesNotExtendGraceAndOlderFailureTightensIt() {
		Subscription subscription = activeOrg();
		Instant first = T0.plusSeconds(10);
		subscription.establishGrace(attention(subscription, ProviderCollectionState.PAST_DUE, first), first, CLOCK);
		Instant original = subscription.graceEndsAt();

		assertThat(subscription.tightenGraceDeadline(first.plusSeconds(60), CLOCK)).isFalse();
		assertThat(subscription.tightenGraceDeadline(first, CLOCK)).isFalse();
		Instant older = T0;
		assertThat(subscription.tightenGraceDeadline(older, CLOCK)).isTrue();
		assertThat(subscription.graceEndsAt()).isEqualTo(BillingPolicies.graceDeadline(older));
		assertThat(subscription.graceEndsAt()).isBefore(original);
	}

	@Test
	void pastDueCannotReceiveANewGraceWindow() {
		Subscription subscription = activeOrg();
		subscription.recordExceptionalPaymentAttention(
				attention(subscription, ProviderCollectionState.PAUSED, T0.plusSeconds(1)), CLOCK);

		assertThatThrownBy(() -> subscription.establishGrace(
				attention(subscription, ProviderCollectionState.PAST_DUE, T0.plusSeconds(2)),
				T0.plusSeconds(2),
				CLOCK))
				.isInstanceOf(IllegalSubscriptionTransitionException.class);
		assertThat(subscription.graceEndsAt()).isNull();
	}

	@Test
	void recoveryClearsGraceAndExpiredDoesNotResurrect() {
		Subscription subscription = activeOrg();
		subscription.establishGrace(attention(subscription, ProviderCollectionState.PAST_DUE, T0.plusSeconds(1)), T0, CLOCK);
		ProviderSubscriptionSnapshot recovered = activeSnapshot(T0.plusSeconds(5));
		assertThat(subscription.synchronizeProviderSnapshot(recovered, CLOCK)).isTrue();
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(subscription.graceEndsAt()).isNull();

		subscription.expire(CLOCK);
		assertThat(subscription.synchronizeProviderSnapshot(activeSnapshot(T0.plusSeconds(9)), CLOCK)).isFalse();
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
	}

	@Test
	void earlyProviderTerminalStateDoesNotEndOpenGrace() {
		Subscription subscription = activeOrg();
		Instant failure = T0;
		subscription.establishGrace(attention(subscription, ProviderCollectionState.PAST_DUE, T0.plusSeconds(1)), failure, CLOCK);
		Instant deadline = subscription.graceEndsAt();
		ProviderSubscriptionSnapshot ended = new ProviderSubscriptionSnapshot(
				"cus_active",
				"sub_active",
				ProviderCommercialStatus.ENDED,
				false,
				null,
				deadline,
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				T0.plusSeconds(3));

		assertThat(subscription.synchronizeProviderSnapshot(ended, CLOCK)).isTrue();
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
		assertThat(subscription.graceEndsAt()).isEqualTo(deadline);
		assertThat(subscription.isCommerciallyEntitledAt(T0.plusSeconds(3))).isTrue();
	}

	@Test
	void elapsedGraceExpiresWhenProviderIsAlreadyTerminal() {
		Subscription subscription = activeOrg();
		subscription.establishGrace(attention(subscription, ProviderCollectionState.PAST_DUE, T0.plusSeconds(1)), T0, CLOCK);
		Instant deadline = subscription.graceEndsAt();
		Clock later = Clock.fixed(deadline.plusSeconds(1), ZoneOffset.UTC);
		ProviderSubscriptionSnapshot ended = new ProviderSubscriptionSnapshot(
				subscription.providerCustomerRef(),
				subscription.providerSubscriptionRef(),
				ProviderCommercialStatus.ENDED,
				false,
				null,
				deadline,
				subscription.planKey(),
				subscription.billingCadence(),
				deadline.plusSeconds(1));

		assertThat(subscription.synchronizeProviderSnapshot(ended, later)).isTrue();
		assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		assertThat(subscription.graceEndsAt()).isNull();
	}

	private static Subscription activeOrg() {
		Subscription subscription = pendingOrg();
		subscription.activate(T0.plusSeconds(30 * 24 * 60 * 60), CLOCK);
		subscription.attachProviderReferences("cus_active", "sub_active", CLOCK);
		return subscription;
	}

	private static Subscription pendingCheckout() {
		return Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(UUID.randomUUID()),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				CLOCK);
	}

	private static ProviderSubscriptionSnapshot attention(
			Subscription subscription,
			ProviderCollectionState collection,
			Instant providerAsOf) {
		return new ProviderSubscriptionSnapshot(
				subscription.providerCustomerRef() == null ? "cus_attention" : subscription.providerCustomerRef(),
				subscription.providerSubscriptionRef() == null ? "sub_attention" : subscription.providerSubscriptionRef(),
				ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED,
				false,
				subscription.trialEndsAt(),
				subscription.currentPeriodEndsAt() == null
						? T0.plusSeconds(30 * 24 * 60 * 60)
						: subscription.currentPeriodEndsAt(),
				subscription.planKey(),
				subscription.billingCadence() == null ? BillingCadence.MONTHLY : subscription.billingCadence(),
				providerAsOf,
				collection);
	}

	private static ProviderSubscriptionSnapshot activeSnapshot(Instant providerAsOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_active",
				"sub_active",
				ProviderCommercialStatus.ACTIVE,
				false,
				null,
				T0.plusSeconds(30 * 24 * 60 * 60),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				providerAsOf);
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
