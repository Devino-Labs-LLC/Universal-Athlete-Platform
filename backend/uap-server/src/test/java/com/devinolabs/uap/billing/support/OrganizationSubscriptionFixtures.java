package com.devinolabs.uap.billing.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

/**
 * Local domain subscription fixtures. No Stripe network calls.
 */
public final class OrganizationSubscriptionFixtures {

	private OrganizationSubscriptionFixtures() {
	}

	public static Subscription saveOrganizationState(
			SubscriptionRepository repository,
			UUID organizationId,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			Instant asOf) {
		Subscription subscription = Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY,
				state,
				null,
				null,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				null,
				asOf,
				asOf,
				0L);
		return repository.save(subscription);
	}

	public static Subscription saveActiveOrganization(SubscriptionRepository repository, UUID organizationId, Clock clock) {
		Instant now = Instant.now(clock);
		Subscription pending = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.ORG_BAND_25,
				clock);
		pending.activate(now.plus(Duration.ofDays(30)), clock);
		return repository.save(pending);
	}

	public static Subscription savePendingOrganization(SubscriptionRepository repository, UUID organizationId, Clock clock) {
		return repository.save(Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.ORG_BAND_25,
				clock));
	}

	public static Subscription saveExpiredOrganization(SubscriptionRepository repository, UUID organizationId, Clock clock) {
		Subscription subscription = saveActiveOrganization(repository, organizationId, clock);
		subscription.expire(clock);
		return repository.save(subscription);
	}

	public static Subscription savePastDueOrganization(SubscriptionRepository repository, UUID organizationId, Clock clock) {
		Subscription subscription = saveActiveOrganization(repository, organizationId, clock);
		subscription.markPastDue(clock);
		return repository.save(subscription);
	}

	public static Subscription saveIndividualPremium(SubscriptionRepository repository, UUID accountId, Clock clock) {
		Subscription pending = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.GOOGLE_PLAY,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				clock);
		pending.activate(Instant.now(clock).plus(Duration.ofDays(30)), clock);
		return repository.save(pending);
	}
}
