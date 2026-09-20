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

import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

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
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public OrganizationWebhookService(
			OrganizationBillingProvider billingProvider,
			ProviderEventInbox eventInbox,
			SubscriptionRepository subscriptionRepository,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.billingProvider = Objects.requireNonNull(billingProvider);
		this.eventInbox = Objects.requireNonNull(eventInbox);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
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
		for (int attempt = 1; attempt <= MAX_APPLY_ATTEMPTS; attempt++) {
			try {
				billingTransactions.executeWithoutResult(status -> {
					ProviderEventProcessingStatus outcome = apply(event);
					eventInbox.complete(claimed.id(), outcome, Instant.now(clock));
				});
				return;
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				lastConflict = ex;
			}
		}
		throw lastConflict;
	}

	private ProviderEventProcessingStatus apply(OrganizationBillingProvider.VerifiedProviderEvent event) {
		if (event.liveMode() || !HANDLED_EVENT_TYPES.contains(event.eventType())
				|| event.subscriptionId() == null || event.organizationId() == null) {
			return ProviderEventProcessingStatus.IGNORED;
		}
		Subscription subscription = subscriptionRepository.findById(SubscriptionId.of(event.subscriptionId()))
				.orElse(null);
		if (subscription == null
				|| subscription.subject().subjectId() == null
				|| !event.organizationId().equals(subscription.subject().subjectId())
				|| subscription.provider() != BillingProvider.STRIPE) {
			return ProviderEventProcessingStatus.IGNORED;
		}
		ProviderSubscriptionSnapshot snapshot = billingProvider.fetchAuthoritativeSnapshot(event);
		boolean changed = subscription.synchronizeProviderSnapshot(snapshot, clock);
		if (changed) {
			Subscription saved = subscriptionRepository.save(subscription);
			if (saved.lifecycleState() == SubscriptionLifecycleState.TRIALING
					|| saved.lifecycleState() == SubscriptionLifecycleState.ACTIVE) {
				auditPort.subscriptionActivated(saved.id().value(), event.organizationId(), saved.lifecycleState());
			}
			else {
				auditPort.subscriptionSynchronized(
						saved.id().value(),
						event.organizationId(),
						null,
						saved.lifecycleState());
			}
		}
		return ProviderEventProcessingStatus.PROCESSED;
	}

}
