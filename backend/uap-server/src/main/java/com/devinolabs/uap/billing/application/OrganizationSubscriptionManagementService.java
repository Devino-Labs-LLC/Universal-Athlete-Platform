package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider.PortalSession;
import com.devinolabs.uap.billing.application.OrganizationCheckoutService.SubscriptionResult;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationAthleteBand;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class OrganizationSubscriptionManagementService {

	private static final Logger log = LoggerFactory.getLogger(OrganizationSubscriptionManagementService.class);
	private static final int ATTEMPTS = 3;
	private static final String NOT_MANAGEABLE = "BILLING_SUBSCRIPTION_NOT_MANAGEABLE";
	private static final String LIFECYCLE = "BILLING_LIFECYCLE_CONFLICT";
	private static final String CAPACITY = "ORGANIZATION_PLAN_CAPACITY_CONFLICT";
	private static final String CAPACITY_MESSAGE =
			"The selected Organization plan does not cover the current active athletes";

	private final OrganizationMembershipPort membershipPort;
	private final OrganizationBillingCustomerRepository customerRepository;
	private final SubscriptionRepository subscriptionRepository;
	private final OrganizationBillingProvider billingProvider;
	private final BillingAuditPort auditPort;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public OrganizationSubscriptionManagementService(
			OrganizationMembershipPort membershipPort,
			OrganizationBillingCustomerRepository customerRepository,
			SubscriptionRepository subscriptionRepository,
			OrganizationBillingProvider billingProvider,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.membershipPort = Objects.requireNonNull(membershipPort);
		this.customerRepository = Objects.requireNonNull(customerRepository);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	public PortalSessionResult openPortal(UUID actorAccountId, UUID organizationId) {
		requireOwner(actorAccountId, organizationId);
		requireSingleOpenSubscription(organizationId);
		OrganizationBillingCustomer customer = customerRepository.findByOrganizationId(organizationId)
				.orElseThrow(() -> conflict(NOT_MANAGEABLE, "Organization billing cannot be managed"));
		PortalSession session = billingProvider.createPortalSession(organizationId, customer.providerCustomerRef());
		return new PortalSessionResult(session.hostedUrl());
	}

	public SubscriptionResult changePlan(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			UUID requestId,
			CommercialPlanKey targetPlanKey,
			BillingCadence targetCadence) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Objects.requireNonNull(targetCadence, "targetCadence must not be null");
		Subscription current = authorize(actorAccountId, organizationId, subscriptionId);
		requireOrganizationPlan(targetPlanKey);
		requirePlanChangeAllowed(current);
		if (current.planKey() == targetPlanKey && current.billingCadence() == targetCadence) {
			return SubscriptionResult.from(current);
		}
		requireProviderSubscription(current);
		boolean sameBand = current.planKey() == targetPlanKey;
		if (!sameBand
				&& lockedAthleteCount(organizationId)
						> OrganizationAthleteBand.forPlan(targetPlanKey).maxActiveAthletes()) {
			throw conflict(CAPACITY, CAPACITY_MESSAGE);
		}
		CommercialPlanKey previousPlan = current.planKey();
		BillingCadence previousCadence = current.billingCadence();
		ProviderSubscriptionSnapshot snapshot = billingProvider.changeSubscriptionPlan(
				subscriptionId,
				current.providerSubscriptionRef(),
				targetPlanKey,
				targetCadence,
				requestId);
		return persistPlanChange(
				actorAccountId,
				organizationId,
				subscriptionId,
				requestId,
				snapshot,
				previousPlan,
				previousCadence,
				targetPlanKey);
	}

	public SubscriptionResult cancel(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Subscription current = authorize(actorAccountId, organizationId, subscriptionId);
		if (current.lifecycleState() == SubscriptionLifecycleState.CANCEL_AT_PERIOD_END) {
			requireFuturePeriodEnd(current);
			return SubscriptionResult.from(current);
		}
		requireCancelAllowed(current);
		requireProviderSubscription(current);
		ProviderSubscriptionSnapshot snapshot = billingProvider.scheduleCancelAtPeriodEnd(
				subscriptionId, current.providerSubscriptionRef(), requestId);
		return persistMutation(actorAccountId, organizationId, subscriptionId, snapshot);
	}

	public SubscriptionResult reactivate(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			UUID requestId) {
		Objects.requireNonNull(requestId, "requestId must not be null");
		Subscription current = authorize(actorAccountId, organizationId, subscriptionId);
		if (current.lifecycleState() == SubscriptionLifecycleState.ACTIVE
				|| current.lifecycleState() == SubscriptionLifecycleState.TRIALING) {
			return SubscriptionResult.from(current);
		}
		requireReactivateAllowed(current);
		requireProviderSubscription(current);
		ProviderSubscriptionSnapshot snapshot = billingProvider.reactivateSubscription(
				subscriptionId, current.providerSubscriptionRef(), requestId);
		return persistMutation(actorAccountId, organizationId, subscriptionId, snapshot);
	}

	/**
	 * External or webhook Price that shrinks the band below current usage is restored to the
	 * previous allow-listed plan. Checkout activation from PENDING is not compensated.
	 */
	public void compensateExternalDowngrade(
			UUID organizationId,
			UUID subscriptionId,
			CommercialPlanKey previousPlan,
			BillingCadence previousCadence,
			String providerSubscriptionRef,
			String operationToken) {
		try {
			ProviderSubscriptionSnapshot restored = restoreWithRetry(
					subscriptionId, providerSubscriptionRef, previousPlan, previousCadence, operationToken);
			persistMutation(null, organizationId, subscriptionId, restored);
		}
		catch (BillingProviderUnavailableException ex) {
			log.error(
					"Organization billing compensation failed for organization {} from plan {} cadence {}",
					organizationId,
					previousPlan,
					previousCadence);
			throw ex;
		}
	}

	public boolean externalDowngradeNeedsCompensation(
			Subscription current,
			ProviderSubscriptionSnapshot incoming,
			long activeAthleteCount) {
		if (current.lifecycleState() == SubscriptionLifecycleState.PENDING
				|| current.lifecycleState() == SubscriptionLifecycleState.EXPIRED
				|| current.billingCadence() == null
				|| !incoming.planKey().isOrganizationPlan()
				|| !current.planKey().isOrganizationPlan()) {
			return false;
		}
		int targetMax = OrganizationAthleteBand.forPlan(incoming.planKey()).maxActiveAthletes();
		int currentMax = OrganizationAthleteBand.forPlan(current.planKey()).maxActiveAthletes();
		return targetMax < currentMax && activeAthleteCount > targetMax;
	}

	private SubscriptionResult persistPlanChange(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			UUID requestId,
			ProviderSubscriptionSnapshot snapshot,
			CommercialPlanKey previousPlan,
			BillingCadence previousCadence,
			CommercialPlanKey targetPlanKey) {
		boolean downgrade = OrganizationAthleteBand.forPlan(targetPlanKey).maxActiveAthletes()
				< OrganizationAthleteBand.forPlan(previousPlan).maxActiveAthletes();
		int targetMax = OrganizationAthleteBand.forPlan(targetPlanKey).maxActiveAthletes();
		ObjectOptimisticLockingFailureException last = null;
		ProviderSubscriptionSnapshot current = snapshot;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				ProviderSubscriptionSnapshot applying = current;
				PersistDecision decision = billingTransactions.execute(status -> {
					long count = membershipPort.lockAndCountDistinctActiveAthletes(organizationId)
							.orElseThrow(BillingOrganizationNotFoundException::new);
					PersistDecision made = new PersistDecision();
					if (downgrade && count > targetMax) {
						made.compensate = true;
						return made;
					}
					made.result = applySnapshot(actorAccountId, organizationId, subscriptionId, applying);
					return made;
				});
				if (decision != null && decision.compensate) {
					return compensateDowngrade(
							organizationId, subscriptionId, requestId, snapshot, previousPlan, previousCadence);
				}
				return decision == null ? SubscriptionResult.from(reload(subscriptionId)) : decision.result;
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				last = ex;
				current = billingProvider.fetchSubscription(snapshot.providerSubscriptionRef());
			}
		}
		throw last;
	}

	private SubscriptionResult compensateDowngrade(
			UUID organizationId,
			UUID subscriptionId,
			UUID requestId,
			ProviderSubscriptionSnapshot undersized,
			CommercialPlanKey previousPlan,
			BillingCadence previousCadence) {
		try {
			ProviderSubscriptionSnapshot restored = restoreWithRetry(
					subscriptionId,
					undersized.providerSubscriptionRef(),
					previousPlan,
					previousCadence,
					requestId.toString());
			persistMutation(null, organizationId, subscriptionId, restored);
			throw conflict(CAPACITY, CAPACITY_MESSAGE);
		}
		catch (BillingProviderUnavailableException ex) {
			log.error(
					"Organization billing compensation failed for organization {} from plan {} cadence {}",
					organizationId,
					previousPlan,
					previousCadence);
			throw ex;
		}
	}

	private ProviderSubscriptionSnapshot restoreWithRetry(
			UUID subscriptionId,
			String providerSubscriptionRef,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			String operationToken) {
		BillingProviderUnavailableException last = null;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				return billingProvider.restoreSubscriptionPlan(
						subscriptionId, providerSubscriptionRef, planKey, cadence, operationToken);
			}
			catch (BillingProviderUnavailableException ex) {
				last = ex;
			}
		}
		throw last;
	}

	private SubscriptionResult persistMutation(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			ProviderSubscriptionSnapshot snapshot) {
		ObjectOptimisticLockingFailureException last = null;
		ProviderSubscriptionSnapshot current = snapshot;
		for (int attempt = 1; attempt <= ATTEMPTS; attempt++) {
			try {
				ProviderSubscriptionSnapshot applying = current;
				return billingTransactions.execute(status -> applySnapshot(
						actorAccountId, organizationId, subscriptionId, applying));
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				last = ex;
				current = billingProvider.fetchSubscription(snapshot.providerSubscriptionRef());
			}
		}
		throw last;
	}

	private SubscriptionResult applySnapshot(
			UUID actorAccountId,
			UUID organizationId,
			UUID subscriptionId,
			ProviderSubscriptionSnapshot snapshot) {
		membershipPort.lockAndCountDistinctActiveAthletes(organizationId)
				.orElseThrow(BillingOrganizationNotFoundException::new);
		Subscription subscription = reload(subscriptionId);
		CommercialPlanKey fromPlan = subscription.planKey();
		BillingCadence fromCadence = subscription.billingCadence();
		SubscriptionLifecycleState fromState = subscription.lifecycleState();
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		if (changed) {
			subscriptionRepository.save(subscription);
			BillingSnapshotAudit.record(
					auditPort,
					subscriptionId,
					organizationId,
					actorAccountId,
					fromPlan,
					fromCadence,
					fromState,
					subscription);
		}
		return SubscriptionResult.from(subscription);
	}

	private Subscription reload(UUID subscriptionId) {
		return subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingOrganizationNotFoundException::new);
	}

	private static final class PersistDecision {
		private boolean compensate;
		private SubscriptionResult result;
	}

	private long lockedAthleteCount(UUID organizationId) {
		return billingTransactions.execute(status -> membershipPort.lockAndCountDistinctActiveAthletes(organizationId)
				.orElseThrow(BillingOrganizationNotFoundException::new));
	}

	private Subscription authorize(UUID actorAccountId, UUID organizationId, UUID subscriptionId) {
		requireOwner(actorAccountId, organizationId);
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(subscriptionId))
				.orElseThrow(BillingOrganizationNotFoundException::new);
		if (subscription.subject().type() != BillingSubjectType.ORGANIZATION
				|| !organizationId.equals(subscription.subject().subjectId())
				|| subscription.provider() != BillingProvider.STRIPE
				|| !subscription.planKey().isOrganizationPlan()) {
			throw new BillingOrganizationNotFoundException();
		}
		requireSingleOpenSubscription(organizationId);
		return subscription;
	}

	private void requireSingleOpenSubscription(UUID organizationId) {
		List<Subscription> open = subscriptionRepository.findBySubject(BillingSubjectType.ORGANIZATION, organizationId)
				.stream()
				.filter(subscription -> subscription.lifecycleState() != SubscriptionLifecycleState.EXPIRED)
				.toList();
		if (open.size() > 1) {
			throw conflict("BILLING_SUBSCRIPTION_STATE_CONFLICT", "Organization billing state is ambiguous");
		}
	}

	private void requirePlanChangeAllowed(Subscription subscription) {
		switch (subscription.lifecycleState()) {
			case ACTIVE, TRIALING -> {
				// manageable
			}
			case PENDING -> throw conflict(
					"BILLING_CHECKOUT_IN_PROGRESS", "Organization already has an open commercial relationship");
			case PAST_DUE, GRACE_PERIOD, CANCEL_AT_PERIOD_END -> throw conflict(
					LIFECYCLE, "This subscription cannot be changed in its current state");
			case EXPIRED -> throw conflict(NOT_MANAGEABLE, "This subscription cannot be managed");
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

	private void requireOwner(UUID actorAccountId, UUID organizationId) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		if (!membershipPort.canManageOrganization(actorAccountId, organizationId)) {
			throw new BillingOrganizationNotFoundException();
		}
	}

	private static void requireOrganizationPlan(CommercialPlanKey planKey) {
		if (planKey == null || !planKey.isOrganizationPlan()) {
			throw new IllegalArgumentException("planKey must be an Organization commercial plan");
		}
	}

	private static BillingConflictException conflict(String code, String message) {
		return new BillingConflictException(code, message);
	}

	public record PortalSessionResult(String url) {
	}

}
