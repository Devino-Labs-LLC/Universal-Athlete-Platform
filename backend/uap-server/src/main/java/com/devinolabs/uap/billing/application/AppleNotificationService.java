package com.devinolabs.uap.billing.application;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedNotification;
import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

/**
 * App Store Server Notifications V2 → {@code billing_provider_events} inbox with
 * {@link BillingProvider#APPLE_APP_STORE}, then canonical Individual Premium lifecycle sync.
 * Ownership bind happens only on authenticated validate/restore — unknown originals are ignored.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
public class AppleNotificationService {

	static final Set<String> HANDLED_NOTIFICATION_TYPES = Set.of(
			"SUBSCRIBED",
			"DID_RENEW",
			"DID_CHANGE_RENEWAL_STATUS",
			"DID_FAIL_TO_RENEW",
			"EXPIRED",
			"GRACE_PERIOD_EXPIRED",
			"REFUND",
			"REVOKE",
			"OFFER_REDEEMED",
			"RENEWAL_EXTENDED");

	private static final int MAX_APPLY_ATTEMPTS = 3;

	private final AppleAppStoreBillingProvider appleProvider;
	private final ProviderEventInbox eventInbox;
	private final SubscriptionRepository subscriptionRepository;
	private final BillingAuditPort auditPort;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public AppleNotificationService(
			AppleAppStoreBillingProvider appleProvider,
			ProviderEventInbox eventInbox,
			SubscriptionRepository subscriptionRepository,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.appleProvider = Objects.requireNonNull(appleProvider);
		this.eventInbox = Objects.requireNonNull(eventInbox);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	public void handle(byte[] payload) {
		if (payload == null || payload.length == 0) {
			throw new InvalidApplePurchaseException("Apple notification payload is required");
		}
		String signedPayload = new String(payload, StandardCharsets.UTF_8).trim();
		if (signedPayload.isEmpty()) {
			throw new InvalidApplePurchaseException("Apple notification payload is required");
		}

		VerifiedNotification notification;
		try {
			notification = appleProvider.verifySignedNotification(signedPayload);
		}
		catch (InvalidApplePurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidApplePurchaseException("Apple notification could not be verified", ex);
		}

		Instant receivedAt = Instant.now(clock);
		ProviderEventReceipt claimed = eventInbox.tryBegin(
				BillingProvider.APPLE_APP_STORE,
				notification.notificationUUID(),
				notification.notificationType(),
				receivedAt).orElse(null);
		if (claimed == null) {
			return;
		}

		ObjectOptimisticLockingFailureException lastConflict = null;
		for (int attempt = 1; attempt <= MAX_APPLY_ATTEMPTS; attempt++) {
			try {
				VerifiedNotification captured = notification;
				billingTransactions.executeWithoutResult(status -> apply(captured, claimed.id()));
				return;
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				lastConflict = ex;
			}
		}
		throw lastConflict;
	}

	private void apply(VerifiedNotification notification, UUID receiptId) {
		if (!HANDLED_NOTIFICATION_TYPES.contains(notification.notificationType())) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return;
		}

		VerifiedPurchase purchase = notification.purchase();
		Subscription subscription = subscriptionRepository
				.findByProviderAndProviderSubscriptionRef(
						BillingProvider.APPLE_APP_STORE, purchase.originalTransactionId())
				.orElse(null);
		if (subscription == null
				|| subscription.subject().type() != BillingSubjectType.ACCOUNT
				|| subscription.subject().subjectId() == null
				|| subscription.provider() != BillingProvider.APPLE_APP_STORE
				|| subscription.planKey() != CommercialPlanKey.INDIVIDUAL_PREMIUM) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return;
		}

		UUID accountId = subscription.subject().subjectId();
		ProviderSubscriptionSnapshot snapshot = appleProvider.toSnapshot(purchase, accountId);
		boolean changed = applyNotification(subscription, notification.notificationType(), snapshot);
		if (changed) {
			Subscription saved = subscriptionRepository.save(subscription);
			auditPort.accountSubscriptionSynchronized(
					saved.id().value(),
					accountId,
					null,
					saved.lifecycleState());
		}
		eventInbox.complete(receiptId, ProviderEventProcessingStatus.PROCESSED, Instant.now(clock));
	}

	private boolean applyNotification(
			Subscription subscription,
			String notificationType,
			ProviderSubscriptionSnapshot snapshot) {
		if ("DID_FAIL_TO_RENEW".equals(notificationType)) {
			return PaymentRecoveryApplier.apply(
					subscription,
					"invoice.payment_failed",
					snapshot.providerStateAsOf(),
					snapshot,
					clock);
		}
		if (("EXPIRED".equals(notificationType)
				|| "GRACE_PERIOD_EXPIRED".equals(notificationType)
				|| "REFUND".equals(notificationType)
				|| "REVOKE".equals(notificationType))
				&& subscription.lifecycleState() == SubscriptionLifecycleState.PENDING) {
			subscription.expire(clock);
			return true;
		}
		return subscription.synchronizeProviderSnapshot(snapshot, clock);
	}

}
