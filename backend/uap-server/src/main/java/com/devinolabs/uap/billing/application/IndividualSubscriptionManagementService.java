package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.IndividualBillingProvider.PortalSession;
import com.devinolabs.uap.billing.application.IndividualCheckoutService.SubscriptionResult;
import com.devinolabs.uap.billing.domain.AccountBillingCustomer;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

/**
 * Account Individual Premium manage surface mirrors Organization §40: Customer Portal is
 * payment-method / invoice history only; cancel-at-period-end and reactivate are app-owned.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class IndividualSubscriptionManagementService {

	private static final int ATTEMPTS = 3;
	private static final String NOT_MANAGEABLE = "BILLING_SUBSCRIPTION_NOT_MANAGEABLE";
	private static final String LIFECYCLE = "BILLING_LIFECYCLE_CONFLICT";

	private final AccountBillingCustomerRepository customerRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final IndividualBillingProvider billingProvider;
	private final BillingAuditPort auditPort;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public IndividualSubscriptionManagementService(
			AccountBillingCustomerRepository customerRepository,
			SubscriptionRepository subscriptionRepository,
			IndividualBillingProvider billingProvider,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.customerRepository = Objects.requireNonNull(customerRepository);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	public PortalSessionResult openPortal(UUID actorAccountId) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		requireSingleOpenSubscription(actorAccountId);
		AccountBillingCustomer customer = customerRepository.findByAccountId(actorAccountId)
				.orElseThrow(BillingAccountNotFoundException::new);
		PortalSession session = billingProvider.createAccountPortalSession(
				actorAccountId, customer.providerCustomerRef());
		return new PortalSessionResult(session.hostedUrl());
	}

	public SubscriptionResult cancel(UUID actorAccountId, UUID subscriptionId, UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Subscription current = authorize(actorAccountId, subscriptionId);
		if (current.lifecycleState() == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END) {
			requireFuturePeriodEnd(current);
			return SubscriptionResult.from(current);
		}
		requireCancelAllowed(current);
		requireProviderSubscription(current);
		ProviderSubscriptionSnapshot snapshot = billingProvider.scheduleAccountCancelAtPeriodEnd(
				subscriptionId, current.providerSubscriptionRef(), requestId);
		return persistMutation(actorAccountId, subscriptionId, snapshot);
	}

	public SubscriptionResult reactivate(UUID actorAccountId, UUID subscriptionId, UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Subscription current = authorize(actorAccountId, subscriptionId);
		if (current.lifecycleState() == SubscriptionLifecycleState.ACTIVE
				|| current.lifecycleState() == SubscriptionLifecycleState.TRIALING) {
			return SubscriptionResult.from(current);
		}
		requireReactivateAllowed(current);
		requireProviderSubscription(current);
		ProviderSubscriptionSnapshot snapshot = billingProvider.reactivateAccountSubscription(
				subscriptionId, current.providerSubscriptionRef(), requestId);
		return persistMutation(actorAccountId, subscriptionId, snapshot);
	}

	private SubscriptionResult persistMutation(
			UUID actorAccountId,
			UUID subscriptionId,
			ProviderSubscriptionSnapshot snapshot) {
		ObjectOptimisticLockingFailureException last = null;
		ProviderSubscriptionSnapshot current = snapshot;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				ProviderSubscriptionSnapshot applying = current;
				return billingTransactions.execute(status -> applySnapshot(actorAccountId, subscriptionId, applying));
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				last = ex;
				current = billingProvider.fetchAccountSubscription(snapshot.providerSubscriptionRef());
			}
		}
		throw last;
	}

	private SubscriptionResult applySnapshot(
			UUID actorAccountId,
			UUID subscriptionId,
			ProviderSubscriptionSnapshot snapshot) {
		Subscription subscription = reload(subscriptionId);
		requireOwnedStripeIndividualSubscription(subscription, actorAccountId);
		SubscriptionLifecycleState fromState = subscription.lifecycleState();
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		if (changed) {
			subscriptionRepository.save(subscription);
			BillingSnapshotAudit.recordAccount(
					auditPort,
					subscriptionId,
					actorAccountId,
					actorAccountId,
					fromState,
					subscription);
		}
		return SubscriptionResult.from(subscription);
	}

	private Subscription reload(UUID subscriptionId) {
		return subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingAccountNotFoundException::new);
	}

	private Subscription authorize(UUID actorAccountId, UUID subscriptionId) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingAccountNotFoundException::new);
		requireOwnedStripeIndividualSubscription(subscription, actorAccountId);
		requireSingleOpenSubscription(actorAccountId);
		return subscription;
	}

	private void requireSingleOpenSubscription(UUID accountId) {
		List<Subscription> open = subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)
				.stream()
				.filter(subscription -> subscription.lifecycleState() != SubscriptionLifecycleState.EXPIRED)
				.toList();
		if (open.size() > 1) {
			throw conflict("BILLING_SUBSCRIPTION_STATE_CONFLICT", "Account billing state is ambiguous");
		}
	}

	private void requireCancelAllowed(Subscription subscription) {
		switch (subscription.lifecycleState()) {
			case ACTIVE, TRIALING -> {
				// manageable
			}
			case PENDING, EXPIRED -> throw conflict(NOT_MANAGEABLE, "This subscription cannot be managed");
			case PAST_DUE, GRACE_PERIOD -> throw conflict(
					LIFECYCLE, "This subscription cannot be changed in its current state");
			case CANCEL_AT_PERIOD_END -> requireFuturePeriodEnd(subscription);
		}
	}

	private void requireReactivateAllowed(Subscription subscription) {
		if (subscription.lifecycleState() != SubscriptionLifecycleState.CANCEL_AT_PERIOD_END) {
			if (subscription.lifecycleState() == SubscriptionLifecycleState.PAST_DUE
					|| subscription.lifecycleState() == SubscriptionLifecycleState.GRACE_PERIOD) {
				throw conflict(LIFECYCLE, "This subscription cannot be changed in its current state");
			}
			throw conflict(NOT_MANAGEABLE, "This subscription cannot be managed");
		}
		requireFuturePeriodEnd(subscription);
	}

	private void requireFuturePeriodEnd(Subscription subscription) {
		if (subscription.currentPeriodEndsAt() == null
				|| !subscription.currentPeriodEndsAt().isAfter(clock.instant())) {
			throw conflict(NOT_MANAGEABLE, "This subscription cannot be managed");
		}
	}

	private static void requireProviderSubscription(Subscription subscription) {
		if (subscription.providerSubscriptionRef() == null) {
			throw conflict(NOT_MANAGEABLE, "This subscription cannot be managed");
		}
	}

	private static void requireOwnedStripeIndividualSubscription(Subscription subscription, UUID accountId) {
		if (subscription.subject().type() != BillingSubjectType.ACCOUNT
				|| !accountId.equals(subscription.subject().subjectId())
				|| subscription.provider() != BillingProvider.STRIPE
				|| !subscription.planKey().isIndividualPlan()) {
			throw new BillingAccountNotFoundException();
		}
	}

	private static BillingConflictException conflict(String code, String message) {
		return new BillingConflictException(code, message);
	}

	public record PortalSessionResult(String url) {
	}

}
