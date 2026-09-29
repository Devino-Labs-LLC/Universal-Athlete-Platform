package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

/**
 * ADR-045 / §9: one entitled Individual provider at a time. Replacement only after
 * paid-through. Expires temporally ended CANCEL/GRACE leftovers so a switch does not
 * leave a second open row. Does not copy payment credentials across providers.
 */
@Service
public class IndividualSubscriptionConflictService {

	private final SubscriptionRepository subscriptionRepository;
	private final Clock clock;

	public IndividualSubscriptionConflictService(SubscriptionRepository subscriptionRepository, Clock clock) {
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	/**
	 * Clears paid-through leftovers, then rejects any remaining open Individual relationship.
	 *
	 * @param ignoreProviderSubscriptionRef optional same-provider restore ref to skip
	 */
	public void requireClearForNewIndividualPurchase(UUID accountId, String ignoreProviderSubscriptionRef) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Instant now = Instant.now(clock);
		for (Subscription existing : subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)) {
			if (!existing.planKey().isIndividualPlan()) {
				continue;
			}
			if (existing.lifecycleState() == SubscriptionLifecycleState.EXPIRED) {
				continue;
			}
			if (ignoreProviderSubscriptionRef != null
					&& ignoreProviderSubscriptionRef.equals(existing.providerSubscriptionRef())) {
				continue;
			}
			if (isPaidThroughOpen(existing, now)) {
				existing.expire(clock);
				subscriptionRepository.save(existing);
				continue;
			}
			if (existing.lifecycleState() == SubscriptionLifecycleState.PENDING) {
				throw new BillingConflictException(
						"BILLING_CHECKOUT_IN_PROGRESS",
						"Account already has an open Individual checkout");
			}
			throw new BillingConflictException(
					"BILLING_SUBSCRIPTION_EXISTS",
					"Account already has an Individual commercial relationship");
		}
	}

	/**
	 * Temporally ended CANCEL / GRACE rows that recovery has not yet marked EXPIRED.
	 * Still open in persistence, but no longer entitled — switch-eligible after expire.
	 */
	static boolean isPaidThroughOpen(Subscription subscription, Instant asOf) {
		SubscriptionLifecycleState state = subscription.lifecycleState();
		if (state != SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
				&& state != SubscriptionLifecycleState.GRACE_PERIOD) {
			return false;
		}
		return !subscription.isCommerciallyEntitledAt(asOf);
	}
}
