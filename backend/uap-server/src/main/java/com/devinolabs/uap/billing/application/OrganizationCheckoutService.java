package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class OrganizationCheckoutService {

	private final OrganizationMembershipPort membershipPort;
	private final OrganizationBillingCustomerRepository customerRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final OrganizationBillingProvider billingProvider;
	private final BillingAuditPort auditPort;
	private final Clock clock;

	public OrganizationCheckoutService(
			OrganizationMembershipPort membershipPort,
			OrganizationBillingCustomerRepository customerRepository,
			SubscriptionRepository subscriptionRepository,
			OrganizationBillingProvider billingProvider,
			BillingAuditPort auditPort,
			Clock clock) {
		this.membershipPort = Objects.requireNonNull(membershipPort);
		this.customerRepository = Objects.requireNonNull(customerRepository);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public CheckoutResult startCheckout(
			UUID actorAccountId,
			UUID organizationId,
			UUID requestId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		requireOwner(actorAccountId, organizationId);
		requireOrganizationPlan(planKey);
		Objects.requireNonNull(requestId, "requestId must not be null");
		Objects.requireNonNull(cadence, "cadence must not be null");

		OrganizationBillingCustomer customer = lockOrCreateCustomer(organizationId);
		SubscriptionId subscriptionId = SubscriptionId.of(requestId);
		Subscription subscription = subscriptionRepository.findById(subscriptionId).orElse(null);
		boolean created = subscription == null;
		if (created) {
			requireNoOpenSubscription(organizationId);
			subscription = Subscription.startPendingOrganizationCheckout(
					subscriptionId,
					BillingSubject.organization(organizationId),
					planKey,
					cadence,
					clock);
			subscriptionRepository.save(subscription);
		}
		else {
			requireMatchingReplay(subscription, organizationId, planKey, cadence);
		}

		OrganizationBillingProvider.CheckoutSession checkout = billingProvider.createCheckoutSession(
				organizationId,
				requestId,
				customer.providerCustomerRef(),
				planKey,
				cadence);
		if (created) {
			auditPort.checkoutInitiated(requestId, organizationId, actorAccountId, planKey, cadence);
		}
		return new CheckoutResult(requestId, checkout.sessionId(), checkout.checkoutUrl());
	}

	@Transactional
	public SubscriptionResult synchronize(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			String checkoutSessionId) {
		requireOwner(actorAccountId, organizationId);
		OrganizationBillingCustomer customer = customerRepository.findByOrganizationIdForUpdate(organizationId)
				.orElseThrow(BillingOrganizationNotFoundException::new);
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingOrganizationNotFoundException::new);
		requireOwnedStripeOrganizationSubscription(subscription, organizationId);
		if (subscription.billingCadence() == null) {
			throw new BillingConflictException("BILLING_CADENCE_MISSING", "Subscription cadence is unavailable");
		}

		boolean changed = subscription.synchronizeProviderSnapshot(
				billingProvider.fetchCheckoutSubscription(
						organizationId,
						subscriptionId,
						checkoutSessionId,
						customer.providerCustomerRef(),
						subscription.planKey(),
						subscription.billingCadence()),
				clock);
		Subscription saved = changed ? subscriptionRepository.save(subscription) : subscription;
		if (changed) {
			auditPort.subscriptionSynchronized(
					subscriptionId,
					organizationId,
					actorAccountId,
					saved.lifecycleState());
		}
		return SubscriptionResult.from(saved);
	}

	@Transactional(readOnly = true)
	public SubscriptionResult currentStatus(UUID actorAccountId, UUID organizationId) {
		requireOwner(actorAccountId, organizationId);
		return subscriptionRepository.findBySubject(BillingSubjectType.ORGANIZATION, organizationId).stream()
				.filter(subscription -> subscription.lifecycleState() != SubscriptionLifecycleState.EXPIRED)
				.reduce((left, right) -> left.createdAt().isAfter(right.createdAt()) ? left : right)
				.map(SubscriptionResult::from)
				.orElseThrow(BillingOrganizationNotFoundException::new);
	}

	private OrganizationBillingCustomer lockOrCreateCustomer(UUID organizationId) {
		OrganizationBillingCustomer existing = customerRepository.findByOrganizationId(organizationId).orElse(null);
		if (existing == null) {
			String customerRef = billingProvider.createCustomer(organizationId);
			Instant now = Instant.now(clock);
			customerRepository.save(OrganizationBillingCustomer.stripe(organizationId, customerRef, now));
		}
		return customerRepository.findByOrganizationIdForUpdate(organizationId)
				.orElseThrow(() -> new IllegalStateException("Organization billing customer was not persisted"));
	}

	private void requireOwner(UUID actorAccountId, UUID organizationId) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		if (!membershipPort.canManageOrganization(actorAccountId, organizationId)) {
			throw new BillingOrganizationNotFoundException();
		}
	}

	private void requireNoOpenSubscription(UUID organizationId) {
		for (Subscription existing : subscriptionRepository.findBySubject(
				BillingSubjectType.ORGANIZATION, organizationId)) {
			if (existing.lifecycleState() != SubscriptionLifecycleState.EXPIRED) {
				String code = existing.lifecycleState() == SubscriptionLifecycleState.PENDING
						? "BILLING_CHECKOUT_IN_PROGRESS"
						: "BILLING_SUBSCRIPTION_EXISTS";
				throw new BillingConflictException(code, "Organization already has an open commercial relationship");
			}
		}
	}

	private static void requireMatchingReplay(
			Subscription subscription,
			UUID organizationId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		requireOwnedStripeOrganizationSubscription(subscription, organizationId);
		if (subscription.lifecycleState() != SubscriptionLifecycleState.PENDING
				|| subscription.planKey() != planKey
				|| subscription.billingCadence() != cadence) {
			throw new BillingConflictException(
					"BILLING_REQUEST_CONFLICT",
					"Checkout request identifier is already associated with different billing state");
		}
	}

	private static void requireOwnedStripeOrganizationSubscription(
			Subscription subscription,
			UUID organizationId) {
		if (subscription.subject().type() != BillingSubjectType.ORGANIZATION
				|| !subscription.subject().subjectId().equals(organizationId)
				|| subscription.provider() != BillingProvider.STRIPE
				|| !subscription.planKey().isOrganizationPlan()) {
			throw new BillingOrganizationNotFoundException();
		}
	}

	private static void requireOrganizationPlan(CommercialPlanKey planKey) {
		if (planKey == null || !planKey.isOrganizationPlan()) {
			throw new IllegalArgumentException("planKey must be an Organization commercial plan");
		}
	}

	public record CheckoutResult(UUID subscriptionId, String checkoutSessionId, String checkoutUrl) {
	}

	public record SubscriptionResult(
			UUID subscriptionId,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			SubscriptionLifecycleState lifecycleState,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt) {

		static SubscriptionResult from(Subscription subscription) {
			return new SubscriptionResult(
					subscription.id().value(),
					subscription.planKey(),
					subscription.billingCadence(),
					subscription.lifecycleState(),
					subscription.trialEndsAt(),
					subscription.currentPeriodEndsAt());
		}
	}

}
