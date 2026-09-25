package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

@Service
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
public class OrganizationWebhookService {

	static final Set<String> HANDLED_EVENT_TYPES = Set.of(
			"checkout.session.completed",
			"customer.subscription.created",
			"customer.subscription.updated",
			"customer.subscription.deleted",
			"invoice.paid",
			"invoice.payment_failed");

	private static final int MAX_APPLY_ATTEMPTS = 3;

	private final OrganizationBillingProvider billingProvider;
	private final ProviderEventInbox eventInbox;
	private final SubscriptionRepository subscriptionRepository;
	private final BillingAuditPort auditPort;
	private final OrganizationMembershipPort membershipPort;
	private final OrganizationSubscriptionManagementService managementService;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public OrganizationWebhookService(
			OrganizationBillingProvider billingProvider,
			ProviderEventInbox eventInbox,
			SubscriptionRepository subscriptionRepository,
			BillingAuditPort auditPort,
			OrganizationMembershipPort membershipPort,
			OrganizationSubscriptionManagementService managementService,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.eventInbox = Objects.requireNonNull(eventInbox);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.membershipPort = Objects.requireNonNull(membershipPort);
		this.managementService = Objects.requireNonNull(managementService);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	public void handle(byte[] payload, String signatureHeader) {
		OrganizationBillingProvider.VerifiedProviderEvent event =
				billingProvider.verifyWebhook(payload, signatureHeader);
		Instant receivedAt = Instant.now(clock);
		ProviderEventReceipt claimed = eventInbox.tryBegin(
				BillingProvider.STRIPE, event.eventId(), event.eventType(), receivedAt).orElse(null);
		if (claimed == null) {
			return;
		}
		ObjectOptimisticLockingFailureException lastConflict = null;
		WebhookApply applied = null;
		for (int attempt = 1; attempt <= MAX_APPLY_ATTEMPTS; attempt++) {
			try {
				applied = billingTransactions.execute(status -> apply(event, claimed.id()));
				break;
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				lastConflict = ex;
			}
		}
		if (applied == null) {
			throw lastConflict;
		}
		if (applied.compensation() != null) {
			try {
				managementService.compensateExternalDowngrade(
						applied.compensation().organizationId(),
						applied.compensation().subscriptionId(),
						applied.compensation().previousPlan(),
						applied.compensation().previousCadence(),
						applied.compensation().providerSubscriptionRef(),
						event.eventId());
				completeReceipt(claimed.id(), ProviderEventProcessingStatus.PROCESSED);
			}
			catch (RuntimeException ex) {
				completeReceipt(claimed.id(), ProviderEventProcessingStatus.FAILED);
				throw ex;
			}
		}
	}

	private void completeReceipt(UUID receiptId, ProviderEventProcessingStatus status) {
		billingTransactions.executeWithoutResult(transaction ->
				eventInbox.complete(receiptId, status, Instant.now(clock)));
	}

	private WebhookApply apply(OrganizationBillingProvider.VerifiedProviderEvent event, UUID receiptId) {
		if (event.liveMode() || !HANDLED_EVENT_TYPES.contains(event.eventType())) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return WebhookApply.done();
		}
		OrganizationBillingProvider.VerifiedProviderEvent identified = identify(event);
		if (identified.subscriptionId() == null || identified.organizationId() == null) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return WebhookApply.done();
		}
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(identified.subscriptionId()))
				.orElse(null);
		if (subscription == null
				|| subscription.subject().subjectId() == null
				|| !identified.organizationId().equals(subscription.subject().subjectId())
				|| subscription.provider() != BillingProvider.STRIPE
				|| providerRefConflicts(subscription, identified)) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return WebhookApply.done();
		}
		ProviderSubscriptionSnapshot snapshot;
		try {
			snapshot = billingProvider.fetchAuthoritativeSnapshot(identified);
		}
		catch (BillingConflictException ex) {
			if ("BILLING_PROVIDER_PRICE_REJECTED".equals(ex.code())) {
				eventInbox.complete(receiptId, ProviderEventProcessingStatus.FAILED, Instant.now(clock));
				return WebhookApply.done();
			}
			throw ex;
		}
		CommercialPlanKey previousPlan = subscription.planKey();
		BillingCadence previousCadence = subscription.billingCadence();
		SubscriptionLifecycleState previousState = subscription.lifecycleState();
		boolean compensate = compensationRequired(subscription, snapshot, identified.organizationId());
		if (compensate) {
			if (subscription.providerSubscriptionRef() == null || previousCadence == null) {
				eventInbox.complete(receiptId, ProviderEventProcessingStatus.FAILED, Instant.now(clock));
				return WebhookApply.done();
			}
			return WebhookApply.compensate(new ExternalCompensation(
					identified.organizationId(),
					identified.subscriptionId(),
					previousPlan,
					previousCadence,
					subscription.providerSubscriptionRef()));
		}
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		if (changed) {
			Subscription saved = subscriptionRepository.save(subscription);
			if (saved.lifecycleState() == SubscriptionLifecycleState.TRIALING
					|| saved.lifecycleState() == SubscriptionLifecycleState.ACTIVE) {
				auditPort.subscriptionActivated(saved.id().value(), identified.organizationId(), saved.lifecycleState());
			}
			else {
				auditPort.subscriptionSynchronized(
						saved.id().value(),
						identified.organizationId(),
						null,
						saved.lifecycleState());
			}
			BillingSnapshotAudit.record(
					auditPort,
					saved.id().value(),
					identified.organizationId(),
					null,
					previousPlan,
					previousCadence,
					previousState,
					saved);
		}
		eventInbox.complete(receiptId, ProviderEventProcessingStatus.PROCESSED, Instant.now(clock));
		return WebhookApply.done();
	}

	private OrganizationBillingProvider.VerifiedProviderEvent identify(
			OrganizationBillingProvider.VerifiedProviderEvent event) {
		if (event.subscriptionId() != null && event.organizationId() != null) {
			return event;
		}
		if (event.providerSubscriptionRef() == null) {
			return event;
		}
		Subscription match = subscriptionRepository
				.findByProviderAndProviderSubscriptionRef(BillingProvider.STRIPE, event.providerSubscriptionRef())
				.orElse(null);
		if (match == null || match.subject().subjectId() == null || match.provider() != BillingProvider.STRIPE) {
			return event;
		}
		return new OrganizationBillingProvider.VerifiedProviderEvent(
				event.eventId(),
				event.eventType(),
				event.liveMode(),
				event.createdAt(),
				event.checkoutSessionId(),
				event.providerSubscriptionRef(),
				match.subject().subjectId(),
				match.id().value());
	}

	private static boolean providerRefConflicts(
			Subscription subscription,
			OrganizationBillingProvider.VerifiedProviderEvent event) {
		return event.providerSubscriptionRef() != null
				&& subscription.providerSubscriptionRef() != null
				&& !event.providerSubscriptionRef().equals(subscription.providerSubscriptionRef());
	}

	private boolean compensationRequired(
			Subscription subscription,
			ProviderSubscriptionSnapshot snapshot,
			UUID organizationId) {
		if (!managementService.externalDowngradeNeedsCompensation(subscription, snapshot, Long.MAX_VALUE)) {
			return false;
		}
		long count = membershipPort.lockAndCountDistinctActiveAthletes(organizationId).orElse(0L);
		return managementService.externalDowngradeNeedsCompensation(subscription, snapshot, count);
	}

	private record ExternalCompensation(
			UUID organizationId,
			UUID subscriptionId,
			CommercialPlanKey previousPlan,
			BillingCadence previousCadence,
			String providerSubscriptionRef) {
	}

	private record WebhookApply(ExternalCompensation compensation) {

		private static WebhookApply done() {
			return new WebhookApply(null);
		}

		private static WebhookApply compensate(ExternalCompensation compensation) {
			return new WebhookApply(compensation);
		}
	}

}
