package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedNotification;
import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

/**
 * Google Play RTDN → {@code billing_provider_events} inbox with
 * {@link BillingProvider#GOOGLE_PLAY}, then canonical Individual Premium lifecycle sync.
 * Ownership bind happens only on authenticated validate/restore — unknown tokens are ignored.
 */
@Service
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
public class GooglePlayNotificationService {

	static final Set<String> HANDLED_NOTIFICATION_TYPES = Set.of(
			"SUBSCRIPTION_RECOVERED",
			"SUBSCRIPTION_RENEWED",
			"SUBSCRIPTION_CANCELED",
			"SUBSCRIPTION_PURCHASED",
			"SUBSCRIPTION_ON_HOLD",
			"SUBSCRIPTION_IN_GRACE_PERIOD",
			"SUBSCRIPTION_RESTARTED",
			"SUBSCRIPTION_REVOKED",
			"SUBSCRIPTION_EXPIRED");

	private final GooglePlayBillingProvider googleProvider;
	private final ProviderEventInbox eventInbox;
	private final SubscriptionRepository subscriptionRepository;
	private final BillingAuditPort auditPort;
	private final Clock clock;
	private final TransactionTemplate billingTransactions;

	public GooglePlayNotificationService(
			GooglePlayBillingProvider googleProvider,
			ProviderEventInbox eventInbox,
			SubscriptionRepository subscriptionRepository,
			BillingAuditPort auditPort,
			Clock clock,
			TransactionTemplate billingTransactions) {
		this.googleProvider = Objects.requireNonNull(googleProvider);
		this.eventInbox = Objects.requireNonNull(eventInbox);
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
		this.billingTransactions = Objects.requireNonNull(billingTransactions);
	}

	public void handle(byte[] payload) {
		if (payload == null || payload.length == 0) {
			throw new InvalidGooglePlayPurchaseException("Google Play RTDN payload is required");
		}

		VerifiedNotification notification;
		try {
			notification = googleProvider.verifyRtdnPayload(payload);
		}
		catch (InvalidGooglePlayPurchaseException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new InvalidGooglePlayPurchaseException("Google Play RTDN could not be verified", ex);
		}

		Instant receivedAt = Instant.now(clock);
		ProviderEventReceipt claimed = eventInbox.tryBegin(
				BillingProvider.GOOGLE_PLAY,
				notification.eventId(),
				notification.notificationType(),
				receivedAt).orElse(null);
		if (claimed == null) {
			return;
		}

		ProviderNotificationApplySupport.applyWithOptimisticRetry(
				billingTransactions,
				() -> apply(notification, claimed.id()));
	}

	private void apply(VerifiedNotification notification, UUID receiptId) {
		if (!HANDLED_NOTIFICATION_TYPES.contains(notification.notificationType())) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return;
		}

		VerifiedPurchase purchase = notification.purchase();
		Subscription subscription = subscriptionRepository
				.findByProviderAndProviderSubscriptionRef(
						BillingProvider.GOOGLE_PLAY, purchase.purchaseToken())
				.orElse(null);
		if (subscription == null
				|| subscription.subject().type() != BillingSubjectType.ACCOUNT
				|| subscription.subject().subjectId() == null
				|| subscription.provider() != BillingProvider.GOOGLE_PLAY
				|| subscription.planKey() != CommercialPlanKey.INDIVIDUAL_PREMIUM) {
			eventInbox.complete(receiptId, ProviderEventProcessingStatus.IGNORED, Instant.now(clock));
			return;
		}

		UUID accountId = subscription.subject().subjectId();
		ProviderSubscriptionSnapshot snapshot = googleProvider.toSnapshot(purchase, accountId);
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
		if ("SUBSCRIPTION_ON_HOLD".equals(notificationType)
				|| "SUBSCRIPTION_IN_GRACE_PERIOD".equals(notificationType)) {
			return PaymentRecoveryApplier.apply(
					subscription,
					"invoice.payment_failed",
					snapshot.providerStateAsOf(),
					snapshot,
					clock);
		}
		if (("SUBSCRIPTION_EXPIRED".equals(notificationType)
				|| "SUBSCRIPTION_REVOKED".equals(notificationType))
				&& subscription.lifecycleState() == SubscriptionLifecycleState.PENDING) {
			subscription.expire(clock);
			return true;
		}
		return subscription.synchronizeProviderSnapshot(snapshot, clock);
	}

}
