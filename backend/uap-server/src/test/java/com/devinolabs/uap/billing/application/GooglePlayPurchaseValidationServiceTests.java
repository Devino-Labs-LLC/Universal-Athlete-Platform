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

import com.devinolabs.uap.billing.application.GooglePlayBillingProvider.VerifiedPurchase;
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

class GooglePlayPurchaseValidationServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-28T16:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
	private static final String PURCHASE_TOKEN = "google-play-purchase-token-abc123xyz";
	private static final String PRODUCT_ID = "premium.monthly";

	private GooglePlayBillingProvider googleProvider;
	private SubscriptionRepository subscriptionRepository;
	private BillingAuditPort auditPort;
	private GooglePlayPurchaseValidationService service;

	@BeforeEach
	void setUp() {
		googleProvider = mock(GooglePlayBillingProvider.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		auditPort = mock(BillingAuditPort.class);
		service = new GooglePlayPurchaseValidationService(
				googleProvider,
				subscriptionRepository,
				new IndividualSubscriptionConflictService(subscriptionRepository, CLOCK),
				auditPort,
				CLOCK);
	}

	@Test
	void validPurchaseBindsToAuthenticatedAccountAndActivatesPremium() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(accountId.toString());
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, purchase);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(googleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)).thenReturn(List.of());
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		GooglePlayPurchaseValidationService.SubscriptionResult result =
				service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID);

		assertThat(result.provider()).isEqualTo(BillingProvider.GOOGLE_PLAY);
		assertThat(result.managementChannel())
				.isEqualTo(com.devinolabs.uap.billing.domain.IndividualManagementChannel.GOOGLE_PLAY);
		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(result.planKey()).isEqualTo(CommercialPlanKey.INDIVIDUAL_PREMIUM);
		ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
		assertThat(saved.getAllValues().getLast().provider()).isEqualTo(BillingProvider.GOOGLE_PLAY);
		assertThat(saved.getAllValues().getLast().providerSubscriptionRef()).isEqualTo(PURCHASE_TOKEN);
		verify(auditPort).accountCheckoutInitiated(
				any(), eq(accountId), eq(accountId), eq(CommercialPlanKey.INDIVIDUAL_PREMIUM), eq(BillingCadence.MONTHLY));
	}

	@Test
	void newBindWithoutObfuscatedAccountIdFailsClosed() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("obfuscatedExternalAccountId");
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void restoreReplaysExistingOwnedGooglePlaySubscription() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		Subscription existing = ownedGoogle(accountId, SubscriptionLifecycleState.ACTIVE);
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, purchase);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(googleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.of(existing));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		GooglePlayPurchaseValidationService.SubscriptionResult result =
				service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID);

		assertThat(result.subscriptionId()).isEqualTo(existing.id().value());
		verify(auditPort, never()).accountCheckoutInitiated(any(), any(), any(), any(), any());
	}

	@Test
	void fakeTokenFailsClosed() {
		UUID accountId = UUID.randomUUID();
		when(googleProvider.validatePurchase("fake-token-value-1234567890", PRODUCT_ID))
				.thenThrow(new InvalidGooglePlayPurchaseException("rejected"));

		assertThatThrownBy(() -> service.validateOrRestore(accountId, "fake-token-value-1234567890", PRODUCT_ID))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void blankTokenFailsClosed() {
		assertThatThrownBy(() -> service.validateOrRestore(UUID.randomUUID(), "  ", PRODUCT_ID))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class);
		verify(googleProvider, never()).validatePurchase(any(), any());
	}

	@Test
	void foreignAccountOwnershipFailsClosed() {
		UUID actor = UUID.randomUUID();
		UUID owner = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(null);
		Subscription foreign = ownedGoogle(owner, SubscriptionLifecycleState.ACTIVE);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service.validateOrRestore(actor, PURCHASE_TOKEN, PRODUCT_ID))
				.isInstanceOf(BillingAccountNotFoundException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void mismatchedObfuscatedAccountIdFailsClosed() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(UUID.randomUUID().toString());
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID))
				.isInstanceOf(BillingAccountNotFoundException.class);
		verify(subscriptionRepository, never()).save(any());
	}

	@Test
	void blankProductIdFailsClosed() {
		assertThatThrownBy(() -> service.validateOrRestore(UUID.randomUUID(), PURCHASE_TOKEN, "  "))
				.isInstanceOf(InvalidGooglePlayPurchaseException.class)
				.hasMessageContaining("Product id");
		verify(googleProvider, never()).validatePurchase(any(), any());
	}

	@Test
	void paidThroughCancelAllowsNewGooglePlayBindAndExpiresPriorOpenRow() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(accountId.toString());
		ProviderSubscriptionSnapshot snapshot = snapshot(accountId, purchase);
		Subscription paidThroughApple = Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				accountId.toString(),
				"apple-original-prior",
				null,
				NOW.minusSeconds(60),
				null,
				NOW.minusSeconds(120),
				NOW.minusSeconds(3600),
				NOW.minusSeconds(120),
				1L);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(googleProvider.toSnapshot(purchase, accountId)).thenReturn(snapshot);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(paidThroughApple));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		GooglePlayPurchaseValidationService.SubscriptionResult result =
				service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID);

		assertThat(result.provider()).isEqualTo(BillingProvider.GOOGLE_PLAY);
		assertThat(paidThroughApple.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
	}

	@Test
	void activeStripeIndividualBlocksNewGooglePlayBind() {
		UUID accountId = UUID.randomUUID();
		VerifiedPurchase purchase = purchase(accountId.toString());
		Subscription stripe = Subscription.startPendingIndividualCheckout(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		stripe.activate(NOW.plusSeconds(30 * 24 * 60 * 60), CLOCK);
		when(googleProvider.validatePurchase(PURCHASE_TOKEN, PRODUCT_ID)).thenReturn(purchase);
		when(subscriptionRepository.findByProviderAndProviderSubscriptionRef(
				BillingProvider.GOOGLE_PLAY, PURCHASE_TOKEN)).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(stripe));

		assertThatThrownBy(() -> service.validateOrRestore(accountId, PURCHASE_TOKEN, PRODUCT_ID))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
	}

	private static VerifiedPurchase purchase(String obfuscatedExternalAccountId) {
		return new VerifiedPurchase(
				PURCHASE_TOKEN,
				"GPA.1234-5678",
				PRODUCT_ID,
				"com.devinolabs.athletereadiness",
				true,
				obfuscatedExternalAccountId,
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

	private static Subscription ownedGoogle(UUID accountId, SubscriptionLifecycleState state) {
		Subscription subscription = Subscription.startPendingIndividualGooglePlayPurchase(
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
