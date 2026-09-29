package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.devinolabs.uap.billing.application.AppleAppStoreBillingProvider.VerifiedPurchase;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCollectionState;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

class ApplePurchaseValidationServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-28T16:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final String ORIGINAL_TX = "1000000123456789";
	private static final String SIGNED = "header.payload.signature";

	private AppleAppStoreBillingProvider appleProvider;
	private SubscriptionRepository subscriptionRepository;
	private BillingAuditPort auditPort;
	private ApplePurchaseValidationService service;

	@BeforeEach
	void setUp() {
		appleProvider = mock(AppleAppStoreBillingProvider.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		auditPort = mock(BillingAuditPort.class);
		service = new ApplePurchaseValidationService(appleProvider, subscriptionRepository, auditPort, CLOCK);
	}

	@Test
	void validPurchaseBindsToAuthenticatedAccountAndActivatesPremium() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(accountId.toString());
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, purchase);
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(appleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)).thenReturn(List.of());
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ApplePurchaseValidationService.SubscriptionResult result =
				service.validateOrRestore(accountId, SIGNED);

		assertThat(result.provider()).isEqualTo(BillingProvider.APPLE_APP_STORE);
		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(result.planKey()).isEqualTo(CommercialPlanKey.INDIVIDUAL_PREMIUM);
		ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
		assertThat(saved.getAllValues().getLast().provider()).isEqualTo(BillingProvider.APPLE_APP_STORE);
		assertThat(saved.getAllValues().getLast().providerSubscriptionRef()).isEqualTo(ORIGINAL_TX);
		verify(auditPort).accountCheckoutInitiated(
				any(), eq(accountId), eq(accountId), eq(CommercialPlanKey.INDIVIDUAL_PREMIUM), eq(BillingCadence.MONTHLY));
	}

	@Test
	void newBindWithoutAppAccountTokenFailsClosed() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.validateOrRestore(accountId, SIGNED))
				.isInstanceOf(InvalidApplePurchaseException.class)
				.hasMessageContaining("appAccountToken");
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void restoreReplaysExistingOwnedAppleSubscription() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		Subscription existing = ownedApple(accountId, SubscriptionLifecycleState.ACTIVE);
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, purchase);
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(appleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.of(existing));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		ApplePurchaseValidationService.SubscriptionResult result =
				service.validateOrRestore(accountId, SIGNED);

		assertThat(result.subscriptionId()).isEqualTo(existing.id().value());
		verify(auditPort, never()).accountCheckoutInitiated(any(), any(), any(), any(), any());
	}

	@Test
	void fakeTokenFailsClosed() {
		UUID accountId = UUID.randomUUID();
		when(appleProvider.validateSignedTransaction("fake.token.value"))
				.thenThrow(new InvalidApplePurchaseException("rejected"));

		assertThatThrownBy(() -> service.validateOrRestore(accountId, "fake.token.value"))
				.isInstanceOf(InvalidApplePurchaseException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void blankTokenFailsClosed() {
		assertThatThrownBy(() -> service.validateOrRestore(UUID.randomUUID(), "  "))
				.isInstanceOf(InvalidApplePurchaseException.class);
		verify(appleProvider, never()).validateSignedTransaction(any());
	}

	@Test
	void foreignAccountOwnershipFailsClosed() {
		UUID actor = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		Subscription foreign = ownedApple(owner, SubscriptionLifecycleState.ACTIVE);
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service.validateOrRestore(actor, SIGNED))
				.isInstanceOf(BillingAccountNotFoundException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void mismatchedAppAccountTokenFailsClosed() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString());
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.validateOrRestore(accountId, SIGNED))
				.isInstanceOf(BillingAccountNotFoundException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void activeStripeIndividualBlocksNewAppleBind() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(accountId.toString());
		Subscription stripe = Subscription.startPendingIndividualCheckout(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		stripe.activate(NOW.plusSeconds(30 * 24 * 60 * 60), CLOCK);
		when(appleProvider.validateSignedTransaction(SIGNED)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.APPLE_APP_STORE, ORIGINAL_TX)).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(stripe));

		assertThatThrownBy(() -> service.validateOrRestore(accountId, SIGNED))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
	}

	private static VerifiedPurchase purchase(String appAccountToken) {
		return new VerifiedPurchase(
				ORIGINAL_TX,
				"2000000987654321",
				"premium.monthly",
				"com.devinolabs.athletereadiness",
				"Sandbox",
				appAccountToken,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				ProviderCommercialStatus.ACTIVE,
				false,
				NOW.minusSeconds(60),
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW,
				ProviderCollectionState.NONE);
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

	private static Subscription ownedApple(UUID accountId, SubscriptionLifecycleState state) {
		Subscription subscription = Subscription.startPendingIndividualApplePurchase(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		if (state == SubscriptionLifecycleState.ACTIVE) {
			subscription.synchronizeProviderSnapshot(snapshot(accountId, purchase(null)), CLOCK);
		}
		return subscription;
	}

}
