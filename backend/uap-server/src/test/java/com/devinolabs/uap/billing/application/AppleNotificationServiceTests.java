package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedNotification;
import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

class AppleNotificationServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-28T17:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final String ORIGINAL_TX = "1000000123456789";
	private static final String NOTIFICATION_UUID = "notif-uuid-1";
	private static final String SIGNED_PAYLOAD = "header.notification.signature";

	private AppleAppStoreBillingProvider appleProvider;
	private ProviderEventInbox eventInbox;
	private SubscriptionRepository subscriptionRepository;
	private BillingAuditPort auditPort;
	private AppleNotificationService service;

	@BeforeEach
	void setUp() {
		appleProvider = mock(AppleAppStoreBillingProvider.class);
		eventInbox = mock(ProviderEventInbox.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		auditPort = mock(BillingAuditPort.class);
		TransactionTemplate transactions = new TransactionTemplate() {
			@Override
			public <T> T execute(TransactionCallback<T> action) {
				return action.doInTransaction(new SimpleTransactionStatus());
			}

			@Override
			public void executeWithoutResult(java.util.function.Consumer<TransactionStatus> action) {
				action.accept(new SimpleTransactionStatus());
			}
		};
		service = new AppleNotificationService(
				appleProvider,
				eventInbox,
				subscriptionRepository,
				auditPort,
				CLOCK,
				transactions);
	}

	@Test
	void didRenewAppliesSnapshotAndMarksInboxProcessed() {
		UUID accountId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		Subscription existing = ownedActive(accountId);
		VerifiedPurchase purchase = purchase(accountId.toString(), ProviderCommercialStatus.ACTIVE);
		// Renew extends paid-through so synchronize reports a change.
		VerifiedPurchase renewed = new VerifiedPurchase(
				purchase.originalTransactionId(),
				purchase.transactionId(),
				purchase.productId(),
				purchase.bundleId(),
				purchase.environment(),
				purchase.appAccountToken(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				purchase.purchaseDate(),
				NOW.plusSeconds(60 * 24 * 60 * 60),
				NOW.plusSeconds(1),
				purchase.collectionState());
		VerifiedNotification notification = notification("DID_RENEW", renewed);
		ProviderEventReceipt receipt = receipt(receiptId);
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, renewed);

		when(appleProvider.verifySignedNotification(SIGNED_PAYLOAD)).thenReturn(notification);
		when(eventInbox.tryBegin(
				eq(BillingProvider.APPLE_APP_STORE),
				eq(NOTIFICATION_UUID),
				eq("DID_RENEW"),
				eq(NOW))).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.of(existing));
		when(appleProvider.toSnapshot(renewed, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle(SIGNED_PAYLOAD.getBytes(StandardCharsets.UTF_8));

		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.PROCESSED, NOW);
		verify(auditPort).accountSubscriptionSynchronized(
				eq(existing.id().value()), eq(accountId), eq(null), any());
	}

	@Test
	void duplicateNotificationReplayIsIdempotent() {
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString(), ProviderCommercialStatus.ACTIVE);
		when(appleProvider.verifySignedNotification(SIGNED_PAYLOAD))
				.thenReturn(notification("DID_RENEW", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any())).thenReturn(Optional.empty());

		service.handle(SIGNED_PAYLOAD.getBytes(StandardCharsets.UTF_8));

		verify(subscriptionRepository, never()).findByProviderAndProviderSubscriptionRef(any(), any());
		verify(eventInbox, never()).complete(any(), any(), any());
	}

	@Test
	void unknownOriginalTransactionIsIgnoredWithoutCreatingOwnership() {
		UUID receiptId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString(), ProviderCommercialStatus.ACTIVE);
		when(appleProvider.verifySignedNotification(SIGNED_PAYLOAD))
				.thenReturn(notification("SUBSCRIBED", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any()))
				.thenReturn(Optional.of(receipt(receiptId)));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.empty());

		service.handle(SIGNED_PAYLOAD.getBytes(StandardCharsets.UTF_8));

		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.IGNORED, NOW);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void fakeNotificationFailsClosedBeforeInboxClaim() {
		when(appleProvider.verifySignedNotification(any()))
				.thenThrow(new InvalidApplePurchaseException("fake"));

		assertThatThrownBy(() -> service.handle("fake.token.value".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(InvalidApplePurchaseException.class);
		verify(eventInbox, never()).tryBegin(any(), any(), any(), any());
	}

	@Test
	void expireNotificationExpiresPendingOwnedSubscription() {
		UUID accountId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingIndividualApplePurchase(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		VerifiedPurchase purchase = purchase(accountId.toString(), ProviderCommercialStatus.ENDED);
		when(appleProvider.verifySignedNotification(SIGNED_PAYLOAD))
				.thenReturn(notification("EXPIRED", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any()))
				.thenReturn(Optional.of(receipt(receiptId)));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.of(pending));
		when(appleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot(accountId, purchase));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle(SIGNED_PAYLOAD.getBytes(StandardCharsets.UTF_8));

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.PROCESSED, NOW);
	}

	private static VerifiedNotification notification(String type, VerifiedPurchase purchase) {
		return new VerifiedNotification(NOTIFICATION_UUID, type, null, purchase, NOW);
	}

	private static ProviderEventReceipt receipt(UUID id) {
		return new ProviderEventReceipt(
				id,
				BillingProvider.APPLE_APP_STORE,
				NOTIFICATION_UUID,
				"DID_RENEW",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
	}

	private static VerifiedPurchase purchase(String appAccountToken, ProviderCommercialStatus status) {
		return new VerifiedPurchase(
				ORIGINAL_TX,
				"2000000987654321",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				appAccountToken,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				status,
				false,
				NOW.minusSeconds(60),
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW,
				status == ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED
						? ProviderCollectionState.PAST_DUE
						: ProviderCollectionState.NONE);
	}

	private static ProviderSubscriptionSnapshot snapshot(UUID accountId, VerifiedPurchase purchase) {
		return new ProviderSubscriptionSnapshot(
				accountId.toString(),
				purchase.originalTransactionId(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				null,
				purchase.expiresDate(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.providerStateAsOf(),
				purchase.collectionState());
	}

	private static Subscription ownedActive(UUID accountId) {
		Subscription subscription = Subscription.startPendingIndividualApplePurchase(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		subscription.synchronizeProviderSnapshot(
				snapshot(accountId, purchase(accountId.toString(), ProviderCommercialStatus.ACTIVE)),
				CLOCK);
		return subscription;
	}

}
