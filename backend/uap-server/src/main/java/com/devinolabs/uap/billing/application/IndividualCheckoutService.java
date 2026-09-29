package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.domain.AccountBillingCustomer;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.IndividualManagementChannel;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class IndividualCheckoutService {

	private final AccountBillingCustomerRepository customerRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final IndividualBillingProvider billingProvider;
	private final IndividualSubscriptionConflictService conflictService;
	private final BillingAuditPort auditPort;
	private final Clock clock;

	public IndividualCheckoutService(
			AccountBillingCustomerRepository customerRepository,
			SubscriptionRepository subscriptionRepository,
			IndividualBillingProvider billingProvider,
			IndividualSubscriptionConflictService conflictService,
			BillingAuditPort auditPort,
			Clock clock) {
		this.customerRepository = Objects.requireNonNull(customerRepository);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.conflictService = Objects.requireNonNull(conflictService);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public CheckoutResult startCheckout(
			UUID accountId,
			UUID requestId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(requestId, "requestId must not be null");
		Objects.requireNonNull(cadence, "cadence must not be null");
		requireIndividualPremium(planKey);

		AccountBillingCustomer customer = lockOrCreateCustomer(accountId);
		SubscriptionId subscriptionId = SubscriptionId.of(requestId);
		Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
		boolean created = subscription == null;
		if (created) {
			conflictService.requireClearForNewIndividualPurchase(accountId, null);
			subscription = Subscription.startPendingIndividualCheckout(
					subscriptionId,
					BillingSubject.account(accountId),
					planKey,
					cadence,
					clock);
			subscriptionRepository.save(subscription);
		}
		else {
			requireMatchingReplay(subscription, accountId, planKey, cadence);
		}

		IndividualBillingProvider.CheckoutSession checkout = billingProvider.createAccountCheckoutSession(
				accountId,
				requestId,
				customer.providerCustomerRef(),
				planKey,
				cadence);
		if (created) {
			auditPort.accountCheckoutInitiated(requestId, accountId, accountId, planKey, cadence);
		}
		return new CheckoutResult(requestId, checkout.sessionId(), checkout.checkoutUrl());
	}

	@Transactional
	public SubscriptionResult synchronize(
			UUID accountId,
			UUID subscriptionId,
			String checkoutSessionId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		AccountBillingCustomer customer = customerRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(BillingAccountNotFoundException::new);
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingAccountNotFoundException::new);
		requireOwnedStripeIndividualSubscription(subscription, accountId);
		if (subscription.billingCadence() == null) {
			throw new BillingConflictException("BILLING_CADENCE_MISSING", "Subscription cadence is unavailable");
		}

		boolean changed = subscription.synchronizeProviderSnapshot(
				billingProvider.fetchAccountCheckoutSubscription(
						accountId,
						subscriptionId,
						checkoutSessionId,
						customer.providerCustomerRef(),
						subscription.planKey(),
						subscription.billingCadence()),
				clock);
		Subscription saved = changed ? subscriptionRepository.save(subscription) : subscription;
		if (changed) {
			auditPort.accountSubscriptionSynchronized(
					subscriptionId,
					accountId,
					accountId,
					saved.lifecycleState());
		}
		return SubscriptionResult.from(saved);
	}

	@Transactional(readOnly = true)
	public SubscriptionResult currentStatus(UUID accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		var open = subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId).stream()
				.filter(subscription -> subscription.lifecycleState() != SubscriptionLifecycleState.EXPIRED)
				.toList();
		if (open.size() > 1) {
			throw new BillingConflictException(
					"BILLING_SUBSCRIPTION_STATE_CONFLICT",
					"Account billing state is ambiguous");
		}
		if (open.isEmpty()) {
			throw new BillingAccountNotFoundException();
		}
		return SubscriptionResult.from(open.getFirst());
	}

	private AccountBillingCustomer lockOrCreateCustomer(UUID accountId) {
		AccountBillingCustomer existing = customerRepository.findByAccountId(accountId).orElse(null);
		if (existing == null) {
			String customerRef = billingProvider.createAccountCustomer(accountId);
			Instant now = Instant.now(clock);
			customerRepository.save(AccountBillingCustomer.stripe(accountId, customerRef, now));
		}
		return customerRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(() -> new IllegalStateException("Account billing customer was not persisted"));
	}

	private static void requireMatchingReplay(
			Subscription subscription,
			UUID accountId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		requireOwnedStripeIndividualSubscription(subscription, accountId);
		if (subscription.lifecycleState() != SubscriptionLifecycleState.PENDING
				|| subscription.planKey() != planKey
				|| subscription.billingCadence() != cadence) {
			throw new BillingConflictException(
					"BILLING_REQUEST_CONFLICT",
					"Checkout request identifier is already associated with different billing state");
		}
	}

	private static void requireOwnedStripeIndividualSubscription(Subscription subscription, UUID accountId) {
		if (subscription.subject().type() != BillingSubjectType.ACCOUNT
				|| !subscription.subject().subjectId().equals(accountId)
				|| subscription.provider() != BillingProvider.STRIPE
				|| !subscription.planKey().isIndividualPlan()) {
			throw new BillingAccountNotFoundException();
		}
	}

	private static void requireIndividualPremium(CommercialPlanKey planKey) {
		if (planKey != CommercialPlanKey.INDIVIDUAL_PREMIUM) {
			throw new IllegalArgumentException("planKey must be INDIVIDUAL_PREMIUM");
		}
	}

	public record CheckoutResult(UUID subscriptionId, String checkoutSessionId, String checkoutUrl) {
	}

	public record SubscriptionResult(
			UUID subscriptionId,
			BillingProvider provider,
			IndividualManagementChannel managementChannel,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			SubscriptionLifecycleState lifecycleState,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt) {

		static SubscriptionResult from(Subscription subscription) {
			return new SubscriptionResult(
					subscription.id().value(),
					subscription.provider(),
					IndividualManagementChannel.forProvider(subscription.provider()),
					subscription.planKey(),
					subscription.billingCadence(),
					subscription.lifecycleState(),
					subscription.trialEndsAt(),
					subscription.currentPeriodEndsAt(),
					subscription.graceEndsAt());
		}
	}

}
