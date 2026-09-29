package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.domain.AccountBillingCustomer;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;

class IndividualSubscriptionManagementServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Instant PERIOD_END = Instant.parse("2026-10-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private AccountBillingCustomerRepository customerRepository;
	private SubscriptionRepository subscriptionRepository;
	private IndividualBillingProvider billingProvider;
	private BillingAuditPort auditPort;
	private TransactionTemplate billingTransactions;
	private IndividualSubscriptionManagementService service;

	@BeforeEach
	void setUp() {
		customerRepository = mock(AccountBillingCustomerRepository.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		billingProvider = mock(IndividualBillingProvider.class);
		auditPort = mock(BillingAuditPort.class);
		billingTransactions = mock(TransactionTemplate.class);
		when(billingTransactions.execute(any())).thenAnswer(invocation -> {
			TransactionCallback<?> callback = invocation.getArgument(0);
			return callback.doInTransaction(mock(TransactionStatus.class));
		});
		service = new IndividualSubscriptionManagementService(
				customerRepository,
				subscriptionRepository,
				billingProvider,
				auditPort,
				CLOCK,
				billingTransactions);
	}

	@Test
	void openPortalUsesStoredCustomerAndRejectsMissingCustomer() {
		UUID accountId = UUID.randomUUID();
		Subscription active = active(accountId, UUID.randomUUID());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(active));
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.openPortal(accountId))
				.isInstanceOf(BillingAccountNotFoundException.class);

		when(customerRepository.findByAccountId(accountId))
				.thenReturn(Optional.of(customer(accountId)));
		when(billingProvider.createAccountPortalSession(accountId, "cus_test_account"))
				.thenReturn(new IndividualBillingProvider.PortalSession("https://billing.stripe.test/portal"));

		assertThat(service.openPortal(accountId).url()).isEqualTo("https://billing.stripe.test/portal");
		verify(billingProvider).createAccountPortalSession(accountId, "cus_test_account");
	}

	@Test
	void cancelSchedulesPeriodEndAndAudits() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		Subscription active = active(accountId, subscriptionId);
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(active));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(active));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.scheduleAccountCancelAtPeriodEnd(subscriptionId, "sub_test_account", requestId))
				.thenReturn(snapshot(true, ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(2)));

		IndividualCheckoutService.SubscriptionResult result =
				service.cancel(accountId, subscriptionId, requestId);

		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		verify(auditPort).accountCancelRequested(subscriptionId, accountId, accountId);
	}

	@Test
	void cancelIsIdempotentWhenAlreadyCancelAtPeriodEnd() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		Subscription canceling = active(accountId, subscriptionId);
		assertThat(canceling.synchronizeProviderSnapshot(
				snapshot(true, ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(2)), CLOCK)).isTrue();
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(canceling));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(canceling));

		IndividualCheckoutService.SubscriptionResult result =
				service.cancel(accountId, subscriptionId, requestId);

		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.CANCEL_AT_PERIOD_END);
		verify(billingProvider, never()).scheduleAccountCancelAtPeriodEnd(any(), any(), any());
		verify(auditPort, never()).accountCancelRequested(any(), any(), any());
	}

	@Test
	void reactivateClearsCancelAndAudits() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		Subscription canceling = active(accountId, subscriptionId);
		assertThat(canceling.synchronizeProviderSnapshot(
				snapshot(true, ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(2)), CLOCK)).isTrue();
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(canceling));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(canceling));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.reactivateAccountSubscription(subscriptionId, "sub_test_account", requestId))
				.thenReturn(snapshot(false, ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(3)));

		IndividualCheckoutService.SubscriptionResult result =
				service.reactivate(accountId, subscriptionId, requestId);

		assertThat(result.lifecycleState()).isEqualTo(SubscriptionLifecycleState.ACTIVE);
		verify(auditPort).accountSubscriptionReactivated(subscriptionId, accountId, accountId);
	}

	@Test
	void foreignSubscriptionIsNotFound() {
		UUID accountId = UUID.randomUUID();
		UUID foreignAccount = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription foreign = active(foreignAccount, subscriptionId);
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(foreign));

		assertThatThrownBy(() -> service.cancel(accountId, subscriptionId, UUID.randomUUID()))
				.isInstanceOf(BillingAccountNotFoundException.class);
		verify(billingProvider, never()).scheduleAccountCancelAtPeriodEnd(any(), any(), any());
	}

	@Test
	void pendingCannotCancelOrReactivate() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription pending = Subscription.startPendingIndividualCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(pending));

		assertThatThrownBy(() -> service.cancel(accountId, subscriptionId, UUID.randomUUID()))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
		assertThatThrownBy(() -> service.reactivate(accountId, subscriptionId, UUID.randomUUID()))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
	}

	@Test
	void ambiguousOpenSubscriptionsConflictOnPortal() {
		UUID accountId = UUID.randomUUID();
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(active(accountId, UUID.randomUUID()), active(accountId, UUID.randomUUID())));

		assertThatThrownBy(() -> service.openPortal(accountId))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_STATE_CONFLICT"));
		verify(billingProvider, never()).createAccountPortalSession(any(), any());
	}

	@Test
	void reactivateAfterPeriodEndIsRejected() {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		Subscription canceling = active(accountId, subscriptionId);
		assertThat(canceling.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_test_account",
						"sub_test_account",
						ProviderCommercialStatus.ACTIVE,
						true,
						null,
						NOW.minusSeconds(1),
						CommercialPlanKey.INDIVIDUAL_PREMIUM,
						BillingCadence.MONTHLY,
						NOW.plusSeconds(2)),
				CLOCK)).isTrue();
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(canceling));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(canceling));

		assertThatThrownBy(() -> service.reactivate(accountId, subscriptionId, UUID.randomUUID()))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
		verify(billingProvider, never()).reactivateAccountSubscription(any(), any(), any());
	}

	private static AccountBillingCustomer customer(UUID accountId) {
		return AccountBillingCustomer.stripe(accountId, "cus_test_account", NOW);
	}

	private static Subscription active(UUID accountId, UUID subscriptionId) {
		Subscription pending = Subscription.startPendingIndividualCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.account(accountId),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				CLOCK);
		assertThat(pending.synchronizeProviderSnapshot(
				snapshot(false, ProviderCommercialStatus.ACTIVE, NOW.plusSeconds(1)), CLOCK)).isTrue();
		return pending;
	}

	private static ProviderSubscriptionSnapshot snapshot(
			boolean cancelAtPeriodEnd,
			ProviderCommercialStatus status,
			Instant providerStateAsOf) {
		return new ProviderSubscriptionSnapshot(
				"cus_test_account",
				"sub_test_account",
				status,
				cancelAtPeriodEnd,
				null,
				PERIOD_END,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				providerStateAsOf);
	}

}
