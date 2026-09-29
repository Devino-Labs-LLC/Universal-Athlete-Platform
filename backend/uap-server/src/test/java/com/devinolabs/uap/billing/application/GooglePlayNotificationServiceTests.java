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

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedNotification;
import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedPurchase;
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

class GooglePlayNotificationServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-28T17:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final String PURCHASE_TOKEN = "google-play-purchase-token-abc123xyz";
	private static final String EVENT_ID = "pubsub-message-1";
	private static final byte[] PAYLOAD = "{\"message\":{\"data\":\"dGVzdA\",\"messageId\":\"pubsub-message-1\"}}"
			.getBytes(StandardCharsets.UTF_8);

	private GooglePlayBillingProvider googleProvider;
	private ProviderEventInbox eventInbox;
	private SubscriptionRepository subscriptionRepository;
	private BillingAuditPort auditPort;
	private GooglePlayNotificationService service;

	@BeforeEach
	void setUp() {
		googleProvider = mock(GooglePlayBillingProvider.class);
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
		service = new GooglePlayNotificationService(
				googleProvider,
				eventInbox,
				subscriptionRepository,
				auditPort,
				CLOCK,
				transactions);
	}

	@Test
	void renewedAppliesSnapshotAndMarksInboxProcessed() {
		UUID accountId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		Subscription existing = ownedActive(accountId);
		VerifiedPurchase purchase = purchase(accountId.toString(), ProviderCommercialStatus.ACTIVE);
		VerifiedPurchase renewed = new VerifiedPurchase(
				purchase.purchaseToken(),
				purchase.orderId(),
				purchase.productId(),
				purchase.packageName(),
				purchase.testPurchase(),
				purchase.obfuscatedExternalAccountId(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				purchase.startTime(),
				NOW.plusSeconds(60 * 24 * 60 * 60),
				NOW.plusSeconds(1),
				purchase.collectionState());
		VerifiedNotification notification = notification("SUBSCRIPTION_RENEWED", renewed);
		ProviderEventReceipt receipt = receipt(receiptId);
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, renewed);

		when(googleProvider.verifyRtdnPayload(PAYLOAD)).thenReturn(notification);
		when(eventInbox.tryBegin(
				eq(BillingProvider.GOOGLE_PLAY),
				eq(EVENT_ID),
				eq("SUBSCRIPTION_RENEWED"),
				eq(NOW))).thenReturn(Optional.of(receipt));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.of(existing));
		when(googleProvider.toSnapshot(renewed, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle(PAYLOAD);

		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.PROCESSED, NOW);
		verify(auditPort).accountSubscriptionSynchronized(
				eq(existing.id().value()), eq(accountId), eq(null), any());
	}

	@Test
	void duplicateNotificationReplayIsIdempotent() {
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString(), ProviderCommercialStatus.ACTIVE);
		when(googleProvider.verifyRtdnPayload(PAYLOAD))
				.thenReturn(notification("SUBSCRIPTION_RENEWED", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any())).thenReturn(Optional.empty());

		service.handle(PAYLOAD);

		verify(subscriptionRepository, never()).findByProviderAndProviderSubscriptionRef(any(), any());
		verify(eventInbox, never()).complete(any(), any(), any());
	}

	@Test
	void unknownPurchaseTokenIsIgnoredWithoutCreatingOwnership() {
		UUID receiptId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString(), ProviderCommercialStatus.ACTIVE);
		when(googleProvider.verifyRtdnPayload(PAYLOAD))
				.thenReturn(notification("SUBSCRIPTION_PURCHASED", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any()))
				.thenReturn(Optional.of(receipt(receiptId)));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());

		service.handle(PAYLOAD);

		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.IGNORED, NOW);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void fakeNotificationFailsClosedBeforeInboxClaim() {
		when(googleProvider.verifyRtdnPayload(any()))
				.thenThrow(new InvalidGooglePlayPurchaseException("fake"));

		assertThatThrownBy(() -> service.handle("fake-token-value-1234567890".getBytes(StandardCharsets.UTF_8)))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class);
		verify(eventInbox, never()).tryBegin(any(), any(), any(), any());
	}

	@Test
	void expireNotificationExpiresPendingOwnedSubscription() {
		UUID accountId = UUID.randomUUID();
		UUID receiptId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingIndividualGooglePlayPurchase(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		VerifiedPurchase purchase = purchase(accountId.toString(), ProviderCommercialStatus.ENDED);
		when(googleProvider.verifyRtdnPayload(PAYLOAD))
				.thenReturn(notification("SUBSCRIPTION_EXPIRED", purchase));
		when(eventInbox.tryBegin(any(), any(), any(), any()))
				.thenReturn(Optional.of(receipt(receiptId)));
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.of(pending));
		when(googleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot(accountId, purchase));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.handle(PAYLOAD);

		assertThat(pending.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
		verify(eventInbox).complete(receiptId, ProviderEventProcessingStatus.PROCESSED, NOW);
	}

	private static VerifiedNotification notification(String type, VerifiedPurchase purchase) {
		return new VerifiedNotification(EVENT_ID, type, purchase, NOW);
	}

	private static ProviderEventReceipt receipt(UUID id) {
		return new ProviderEventReceipt(
				id,
				BillingProvider.GOOGLE_PLAY,
				EVENT_ID,
				"SUBSCRIPTION_RENEWED",
				NOW,
				null,
				ProviderEventProcessingStatus.RECEIVED);
	}

	private static VerifiedPurchase purchase(String obfuscatedExternalAccountId, ProviderCommercialStatus status) {
		return new VerifiedPurchase(
				PURCHASE_TOKEN,
				"GPA.1234-5678",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				true,
				obfuscatedExternalAccountId,
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
				purchase.purchaseToken(),
				purchase.status(),
				purchase.cancelAtPeriodEnd(),
				null,
				purchase.expiryTime(),
				purchase.planKey(),
				purchase.billingCadence(),
				purchase.providerStateAsOf(),
				purchase.collectionState());
	}

	private static Subscription ownedActive(UUID accountId) {
		Subscription subscription = Subscription.startPendingIndividualGooglePlayPurchase(
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
