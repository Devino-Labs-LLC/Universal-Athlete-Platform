package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.OrganizationBillingProvider.PendingCheckoutInspection;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

/**
 * Stripe-conditional recovery and reconciliation. Entitlement does not wait for this job.
 * The worker does not scan healthy ACTIVE or TRIALING rows and does not invent a grace window.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class OrganizationRecoveryWorker {

	static final int PAGE_SIZE = 50;

	static final Duration PENDING_LOOKUP_MIN_AGE = Duration.ofHours(24);

	private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationRecoveryWorker.class);

	private final SubscriptionRepository subscriptionRepository;
	private final OrganizationBillingCustomerRepository customerRepository;
	private final OrganizationBillingProvider billingProvider;
	private final BillingAuditPort auditPort;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public OrganizationRecoveryWorker(
			SubscriptionRepository subscriptionRepository,
			OrganizationBillingCustomerRepository customerRepository,
			OrganizationBillingProvider billingProvider,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.customerRepository = Objects.requireNonNull(customerRepository);
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	@Scheduled(fixedDelay = 15, timeUnit = TimeUnit.MINUTES)
	public void reconcile() {
		Instant now = Instant.now(clock);
		int dueGrace = reconcileDueGrace(now);
		int pastDue = reconcilePastDue();
		int pending = reconcileStalePending(now);
		int elapsedCancel = reconcileElapsedCancel(now);
		LOGGER.info(
				"Billing recovery reconciled dueGrace={} pastDue={} stalePending={} elapsedCancel={}",
				dueGrace,
				pastDue,
				pending,
				elapsedCancel);
	}

	static String graceExpireKey(UUID subscriptionId, Instant graceEndsAt) {
		return "athlete-readiness:grace-expire:" + subscriptionId + ":" + graceEndsAt.toEpochMilli();
	}

	static String pastDueTerminateKey(UUID subscriptionId) {
		return "athlete-readiness:past-due-terminate:" + subscriptionId;
	}

	private int reconcileDueGrace(Instant now) {
		List<Subscription> due = subscriptionRepository.findDueGrace(now, PAGE_SIZE);
		int handled = 0;
		for (Subscription candidate : due) {
			if (reconcileOneDueGrace(candidate.id().value(), candidate.graceEndsAt())) {
				handled++;
			}
		}
		return handled;
	}

	private boolean reconcileOneDueGrace(UUID subscriptionId, Instant expectedGraceEnd) {
		if (expectedGraceEnd == null) {
			return false;
		}
		Subscription confirmed = billingTransactions.execute(status -> reloadIfDueGrace(subscriptionId, expectedGraceEnd));
		if (confirmed == null || confirmed.providerSubscriptionRef() == null) {
			return false;
		}
		ProviderSubscriptionSnapshot snapshot;
		try {
			snapshot = billingProvider.fetchSubscription(confirmed.providerSubscriptionRef());
			if (needsNonpaymentTermination(snapshot)) {
				snapshot = billingProvider.terminateForNonpayment(
						subscriptionId,
						confirmed.providerSubscriptionRef(),
						graceExpireKey(subscriptionId, expectedGraceEnd));
			}
		}
		catch (BillingProviderUnavailableException ex) {
			LOGGER.warn("Billing recovery provider call failed for a due grace candidate");
			return false;
		}
		return persistSnapshot(subscriptionId, expectedGraceEnd, snapshot);
	}

	private int reconcilePastDue() {
		int handled = 0;
		for (Subscription candidate : subscriptionRepository.findPastDue(PAGE_SIZE)) {
			if (reconcileOnePastDue(candidate.id().value())) {
				handled++;
			}
		}
		return handled;
	}

	private boolean reconcileOnePastDue(UUID subscriptionId) {
		Subscription confirmed = billingTransactions.execute(status -> reloadIfState(subscriptionId, SubscriptionLifecycleState.PAST_DUE));
		if (confirmed == null || confirmed.providerSubscriptionRef() == null || confirmed.graceEndsAt() != null) {
			return false;
		}
		ProviderSubscriptionSnapshot snapshot;
		try {
			snapshot = billingProvider.fetchSubscription(confirmed.providerSubscriptionRef());
			if (snapshot.collectionState() == ProviderCollectionState.UNPAID
					|| snapshot.collectionState() == ProviderCollectionState.PAUSED) {
				snapshot = billingProvider.terminateForNonpayment(
						subscriptionId,
						confirmed.providerSubscriptionRef(),
						pastDueTerminateKey(subscriptionId));
			}
			else if (snapshot.collectionState() == ProviderCollectionState.PAST_DUE) {
				return false;
			}
		}
		catch (BillingProviderUnavailableException ex) {
			LOGGER.warn("Billing recovery provider call failed for a past-due candidate");
			return false;
		}
		return persistSnapshot(subscriptionId, null, snapshot);
	}

	private int reconcileStalePending(Instant now) {
		int handled = 0;
		Instant cutoff = now.minus(PENDING_LOOKUP_MIN_AGE);
		for (Subscription candidate : subscriptionRepository.findStalePending(cutoff, PAGE_SIZE)) {
			if (reconcileOnePending(candidate.id().value(), candidate.subject().subjectId())) {
				handled++;
			}
		}
		return handled;
	}

	private boolean reconcileOnePending(UUID subscriptionId, UUID organizationId) {
		if (organizationId == null) {
			return false;
		}
		OrganizationBillingCustomer customer = customerRepository.findByOrganizationId(organizationId).orElse(null);
		if (customer == null || customer.providerCustomerRef() == null) {
			return false;
		}
		PendingCheckoutInspection inspection;
		try {
			inspection = billingProvider.lookupPendingCheckout(customer.providerCustomerRef(), subscriptionId);
		}
		catch (BillingProviderUnavailableException | BillingConflictException ex) {
			LOGGER.warn("Billing recovery could not classify a pending checkout");
			return false;
		}
		if (inspection.outcome() == PendingCheckoutInspection.Outcome.OPEN
				|| inspection.outcome() == PendingCheckoutInspection.Outcome.NOT_FOUND) {
			return false;
		}
		return Boolean.TRUE.equals(billingTransactions.execute(status -> {
			Subscription current = subscriptionRepository.findById(SubscriptionId.of(subscriptionId)).orElse(null);
			if (current == null || current.lifecycleState() != SubscriptionLifecycleState.PENDING) {
				return false;
			}
			CommercialPlanKey fromPlan = current.planKey();
			BillingCadence fromCadence = current.billingCadence();
			SubscriptionLifecycleState fromState = current.lifecycleState();
			boolean changed = inspection.outcome() == PendingCheckoutInspection.Outcome.COMPLETE
					? current.synchronizeProviderSnapshot(inspection.fulfilledSnapshot(), clock)
					: expirePending(current);
			if (!changed) {
				return false;
			}
			subscriptionRepository.save(current);
			record(current, organizationId, fromPlan, fromCadence, fromState);
			return true;
		}));
	}

	private int reconcileElapsedCancel(Instant now) {
		int handled = 0;
		for (Subscription candidate : subscriptionRepository.findElapsedCancelAtPeriodEnd(now, PAGE_SIZE)) {
			if (reconcileOneElapsedCancel(candidate.id().value())) {
				handled++;
			}
		}
		return handled;
	}

	private boolean reconcileOneElapsedCancel(UUID subscriptionId) {
		Subscription confirmed = billingTransactions.execute(
				status -> reloadIfState(subscriptionId, SubscriptionLifecycleState.CANCEL_AT_PERIOD_END));
		if (confirmed == null || confirmed.providerSubscriptionRef() == null) {
			return false;
		}
		ProviderSubscriptionSnapshot snapshot;
		try {
			snapshot = billingProvider.fetchSubscription(confirmed.providerSubscriptionRef());
		}
		catch (BillingProviderUnavailableException ex) {
			LOGGER.warn("Billing recovery provider call failed for an elapsed cancel candidate");
			return false;
		}
		if (snapshot.status() != ProviderCommercialStatus.ENDED) {
			return false;
		}
		return persistSnapshot(subscriptionId, null, snapshot);
	}

	private Subscription reloadIfDueGrace(UUID subscriptionId, Instant expectedGraceEnd) {
		Subscription current = subscriptionRepository.findById(SubscriptionId.of(subscriptionId)).orElse(null);
		Instant now = Instant.now(clock);
		if (current == null
				|| current.lifecycleState() != SubscriptionLifecycleState.GRACE_PERIOD
				|| current.graceEndsAt() == null
				|| !current.graceEndsAt().equals(expectedGraceEnd)
				|| current.graceEndsAt().isAfter(now)) {
			return null;
		}
		return current;
	}

	private Subscription reloadIfState(UUID subscriptionId, SubscriptionLifecycleState expected) {
		Subscription current = subscriptionRepository.findById(SubscriptionId.of(subscriptionId)).orElse(null);
		if (current == null || current.lifecycleState() != expected) {
			return null;
		}
		return current;
	}

	private boolean persistSnapshot(UUID subscriptionId, Instant expectedGraceEnd, ProviderSubscriptionSnapshot snapshot) {
		try {
			Boolean saved = billingTransactions.execute(status -> {
				Subscription current = subscriptionRepository.findById(SubscriptionId.of(subscriptionId)).orElse(null);
				if (current == null) {
					return false;
				}
				if (expectedGraceEnd != null
						&& (current.lifecycleState() != SubscriptionLifecycleState.GRACE_PERIOD
								|| !expectedGraceEnd.equals(current.graceEndsAt()))) {
					return false;
				}
				CommercialPlanKey fromPlan = current.planKey();
				BillingCadence fromCadence = current.billingCadence();
				SubscriptionLifecycleState fromState = current.lifecycleState();
				if (!current.synchronizeProviderSnapshot(snapshot, clock)) {
					return false;
				}
				subscriptionRepository.save(current);
				record(current, current.subject().subjectId(), fromPlan, fromCadence, fromState);
				return true;
			});
			return Boolean.TRUE.equals(saved);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			LOGGER.warn("Billing recovery lost an optimistic write and will retry on the next tick");
			return false;
		}
	}

	private static boolean needsNonpaymentTermination(ProviderSubscriptionSnapshot snapshot) {
		return snapshot.status() == ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED;
	}

	private boolean expirePending(Subscription current) {
		current.expire(clock);
		return true;
	}

	private void record(
			Subscription saved,
			UUID organizationId,
			CommercialPlanKey fromPlan,
			BillingCadence fromCadence,
			SubscriptionLifecycleState fromState) {
		auditPort.subscriptionSynchronized(saved.id().value(), organizationId, null, saved.lifecycleState());
		BillingSnapshotAudit.record(
				auditPort,
				saved.id().value(),
				organizationId,
				null,
				fromPlan,
				fromCadence,
				fromState,
				saved);
	}

}
