package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

import com.devinolabs.uap.billing.domain.AccountBillingCustomer;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

class IndividualCheckoutServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private AccountBillingCustomerRepository customerRepository;
	private SubscriptionRepository subscriptionRepository;
	private IndividualBillingProvider billingProvider;
	private BillingAuditPort auditPort;
	private IndividualCheckoutService service;

	@BeforeEach
	void setUp() {
		customerRepository = mock(AccountBillingCustomerRepository.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		billingProvider = mock(IndividualBillingProvider.class);
		auditPort = mock(BillingAuditPort.class);
		IndividualSubscriptionConflictService conflictService =
				new IndividualSubscriptionConflictService(subscriptionRepository, CLOCK);
		service = new IndividualCheckoutService(
				customerRepository,
				subscriptionRepository,
				billingProvider,
				conflictService,
				auditPort,
				CLOCK);
	}

	@Test
	void happyCheckoutCreatesPendingIndividualPremiumWithoutTrialOnProvider() {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
		when(billingProvider.createAccountCustomer(accountId)).thenReturn("cus_test_account");
		when(customerRepository.save(any())).thenReturn(customer);
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)).thenReturn(List.of());
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.createAccountCheckoutSession(
				accountId,
				requestId,
				"cus_test_account",
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.thenReturn(new IndividualBillingProvider.CheckoutSession(
						"cs_test_individual", "https://checkout.stripe.test/individual"));

		IndividualCheckoutService.CheckoutResult result = service.startCheckout(
				accountId,
				requestId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY);

		assertThat(result.subscriptionId()).isEqualTo(requestId);
		assertThat(result.checkoutSessionId()).isEqualTo("cs_test_individual");
		ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository).save(saved.capture());
		assertThat(saved.getValue().trialEndsAt()).isNull();
		assertThat(saved.getValue().planKey()).isEqualTo(CommercialPlanKey.INDIVIDUAL_PREMIUM);
		verify(billingProvider).createAccountCheckoutSession(
				eq(accountId),
				eq(requestId),
				eq("cus_test_account"),
				eq(CommercialPlanKey.INDIVIDUAL_PREMIUM),
				eq(BillingCadence.MONTHLY));
		verify(auditPort).accountCheckoutInitiated(
				requestId, accountId, accountId, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY);
	}

	@Test
	void requestIdReplayReusesPendingCheckoutWithoutDuplicateAudit() {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription pending = pending(accountId, requestId);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.of(pending));
		when(billingProvider.createAccountCheckoutSession(any(), any(), any(), any(), any()))
				.thenReturn(new IndividualBillingProvider.CheckoutSession(
						"cs_test_same", "https://checkout.stripe.test/same"));

		service.startCheckout(accountId, requestId, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY);
		service.startCheckout(accountId, requestId, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY);

		verify(billingProvider, times(2)).createAccountCheckoutSession(any(), any(), any(), any(), any());
		verify(subscriptionRepository, never()).save(any());
		verify(auditPort, never()).accountCheckoutInitiated(any(), any(), any(), any(), any());
	}

	@Test
	void conflictingRequestIdReplayIsRejected() {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription pending = pending(accountId, requestId);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.of(pending));

		assertThatThrownBy(() -> service.startCheckout(
				accountId,
				requestId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.ANNUAL))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_REQUEST_CONFLICT"));
		verify(billingProvider, never()).createAccountCheckoutSession(any(), any(), any(), any(), any());
	}

	@Test
	void activeIndividualBlocksDuplicateCheckoutFromAnyProvider() {
		UUID accountId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription activeApple = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				CLOCK);
		activeApple.activate(NOW.plusSeconds(30 * 24 * 60 * 60), CLOCK);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(activeApple));

		assertThatThrownBy(() -> service.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
		verify(billingProvider, never()).createAccountCheckoutSession(any(), any(), any(), any(), any());
	}

	@Test
	void pendingCheckoutBlocksNewRequestId() {
		UUID accountId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription existing = pending(accountId, UUID.randomUUID());
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(existing));

		assertThatThrownBy(() -> service.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_CHECKOUT_IN_PROGRESS"));
	}

	@Test
	void synchronizeAppliesActiveSnapshotWithoutTrial() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription pending = pending(accountId, subscriptionId);
		ProviderSubscriptionSnapshot snapshot = new ProviderSubscriptionSnapshot(
				"cus_test_account",
				"sub_test_account",
				ProviderCommercialStatus.ACTIVE,
				false,
				null,
				NOW.plusSeconds(30 * 24 * 60 * 60),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				NOW.plusSeconds(1));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(billingProvider.fetchAccountCheckoutSubscription(any(), any(), any(), any(), any(), any()))
				.thenReturn(snapshot);
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		IndividualCheckoutService.SubscriptionResult result = service.synchronize(
				accountId, subscriptionId, "cs_test_sync");

		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		assertThat(result.trialEndsAt()).isNull();
		verify(auditPort).accountSubscriptionSynchronized(
				subscriptionId, accountId, accountId, SubscriptionLifecycleState.ACTIVE);
	}

	@Test
	void synchronizeWithoutChangeSkipsAudit() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription active = pending(accountId, subscriptionId);
		ProviderSubscriptionSnapshot snapshot = new ProviderSubscriptionSnapshot(
				"cus_test_account",
				"sub_test_account",
				ProviderCommercialStatus.ACTIVE,
				false,
				null,
				NOW.plusSeconds(30 * 24 * 60 * 60),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				NOW.plusSeconds(1));
		assertThat(active.synchronizeProviderSnapshot(snapshot, CLOCK)).isTrue();
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(active));
		when(billingProvider.fetchAccountCheckoutSubscription(any(), any(), any(), any(), any(), any()))
				.thenReturn(snapshot);

		IndividualCheckoutService.SubscriptionResult result = service.synchronize(
				accountId, subscriptionId, "cs_test_sync");

		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		verify(subscriptionRepository, never()).save(any());
		verify(auditPort, never()).accountSubscriptionSynchronized(any(), any(), any(), any());
	}

	@Test
	void synchronizeRejectsMissingCustomerForeignOwnershipAndMissingCadence() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.synchronize(accountId, subscriptionId, "cs_missing"))
				.isInstanceOf(BillingAccountNotFoundException.class);

		AccountBillingCustomer customer = customer(accountId);
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.synchronize(accountId, subscriptionId, "cs_missing_sub"))
				.isInstanceOf(BillingAccountNotFoundException.class);

		Subscription foreign = pending(UUID.randomUUID(), subscriptionId);
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service.synchronize(accountId, subscriptionId, "cs_foreign"))
				.isInstanceOf(BillingAccountNotFoundException.class);
	}

	@Test
	void currentStatusReturnsOpenSubscriptionAndRejectsAmbiguousOrEmpty() {
		UUID accountId = UUID.randomUUID();
		Subscription open = pending(accountId, UUID.randomUUID());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(open));

		IndividualCheckoutService.SubscriptionResult result = service.currentStatus(accountId);

		assertThat(result.subscriptionId()).isEqualTo(open.id().value());
		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PENDING);

		Subscription second = pending(accountId, UUID.randomUUID());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(open, second));

		assertThatThrownBy(() -> service.currentStatus(accountId))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_STATE_CONFLICT"));

		Subscription expired = pending(accountId, UUID.randomUUID());
		expired.expire(CLOCK);
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(expired));

		assertThatThrownBy(() -> service.currentStatus(accountId))
				.isInstanceOf(BillingAccountNotFoundException.class);
	}

	@Test
	void expiredSubscriptionsDoNotBlockNewCheckout() {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		AccountBillingCustomer customer = customer(accountId);
		Subscription expired = pending(accountId, UUID.randomUUID());
		expired.expire(CLOCK);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(expired));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.createAccountCheckoutSession(any(), any(), any(), any(), any()))
				.thenReturn(new IndividualBillingProvider.CheckoutSession(
						"cs_after_expired", "https://checkout.stripe.test/after-expired"));

		IndividualCheckoutService.CheckoutResult result = service.startCheckout(
				accountId,
				requestId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY);

		assertThat(result.checkoutSessionId()).isEqualTo("cs_after_expired");
		verify(auditPort).accountCheckoutInitiated(
				requestId, accountId, accountId, CommercialPlanKey.INDIVIDUAL_PREMIUM, BillingCadence.MONTHLY);
	}

	@Test
	void nonIndividualPlanIsRejected() {
		assertThatThrownBy(() -> service.startCheckout(
				UUID.randomUUID(),
				UUID.randomUUID(),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("INDIVIDUAL_PREMIUM");
	}

	@Test
	void missingPersistedCustomerAfterCreateFailsFast() {
		UUID accountId = UUID.randomUUID();
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());
		when(billingProvider.createAccountCustomer(accountId)).thenReturn("cus_orphan");
		when(customerRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("was not persisted");
	}

	private static AccountBillingCustomer customer(UUID accountId) {
		return AccountBillingCustomer.stripe(accountId, "cus_test_account", NOW);
	}

	private static Subscription pending(UUID accountId, UUID subscriptionId) {
		return Subscription.startPendingIndividualCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
	}

}
