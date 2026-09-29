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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.billing.domain.AccountBillingCustomer;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.IndividualManagementChannel;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;

/**
 * ADR-045 / §9 G4: overlap rejection, provider switch after paid-through, org ∪ Premium union,
 * management routed to origin provider.
 */
class IndividualAdr045PolicyTests {

	private static final Instant NOW = Instant.parse("2026-09-29T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private SubscriptionRepository subscriptionRepository;
	private AccountBillingCustomerRepository customerRepository;
	private IndividualBillingProvider billingProvider;
	private BillingAuditPort auditPort;
	private IndividualSubscriptionConflictService conflictService;
	private IndividualCheckoutService checkoutService;
	private IndividualSubscriptionManagementService managementService;

	@BeforeEach
	void setUp() {
		subscriptionRepository = mock(SubscriptionRepository.class);
		customerRepository = mock(AccountBillingCustomerRepository.class);
		billingProvider = mock(IndividualBillingProvider.class);
		auditPort = mock(BillingAuditPort.class);
		conflictService = new IndividualSubscriptionConflictService(subscriptionRepository, CLOCK);
		checkoutService = new IndividualCheckoutService(
				customerRepository,
				subscriptionRepository,
				billingProvider,
				conflictService,
				auditPort,
				CLOCK);
		managementService = new IndividualSubscriptionManagementService(
				customerRepository,
				subscriptionRepository,
				billingProvider,
				auditPort,
				CLOCK,
				mock(TransactionTemplate.class));
	}

	@Test
	void activeOrGraceIndividualBlocksCrossProviderCheckout() {
		UUID accountId = UUID.randomUUID();
		Subscription activeApple = rehydrateIndividual(
				accountId,
				BillingProvider.APPLE_APP_STORE,
				SubscriptionLifecycleState.ACTIVE,
				NOW.plusSeconds(30 * 24 * 3600),
				null);
		stubCustomer(accountId);
		when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(activeApple));

		assertThatThrownBy(() -> checkoutService.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
		verify(billingProvider, never()).createAccountCheckoutSession(any(), any(), any(), any(), any());

		Subscription graceGoogle = rehydrateIndividual(
				accountId,
				BillingProvider.GOOGLE_PLAY,
				SubscriptionLifecycleState.GRACE_PERIOD,
				null,
				NOW.plusSeconds(3 * 24 * 3600));
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(graceGoogle));

		assertThatThrownBy(() -> checkoutService.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
	}

	@Test
	void cancelAtPeriodEndBeforePaidThroughBlocksReplacement() {
		UUID accountId = UUID.randomUUID();
		Subscription canceling = rehydrateIndividual(
				accountId,
				BillingProvider.APPLE_APP_STORE,
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				NOW.plusSeconds(7 * 24 * 3600),
				null);
		stubCustomer(accountId);
		when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(canceling));

		assertThatThrownBy(() -> checkoutService.startCheckout(
				accountId,
				UUID.randomUUID(),
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_SUBSCRIPTION_EXISTS"));
	}

	@Test
	void paidThroughCancelAllowsSwitchAndExpiresPriorOpenRow() {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		Subscription paidThroughApple = rehydrateIndividual(
				accountId,
				BillingProvider.APPLE_APP_STORE,
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				NOW.minusSeconds(60),
				null);
		stubCustomer(accountId);
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(paidThroughApple));
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.createAccountCheckoutSession(any(), any(), any(), any(), any()))
				.thenReturn(new IndividualBillingProvider.CheckoutSession(
						"cs_switch", "https://checkout.stripe.test/switch"));

		IndividualCheckoutService.CheckoutResult result = checkoutService.startCheckout(
				accountId,
				requestId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY);

		assertThat(result.checkoutSessionId()).isEqualTo("cs_switch");
		ArgumentCaptor<Subscription> saved = ArgumentCaptor.forClass(Subscription.class);
		verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(saved.capture());
		assertThat(saved.getAllValues().stream()
				.anyMatch(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.EXPIRED
						&& subscription.provider() == BillingProvider.APPLE_APP_STORE)).isTrue();
		assertThat(saved.getAllValues().stream()
				.anyMatch(subscription -> subscription.lifecycleState() == SubscriptionLifecycleState.PENDING
						&& subscription.provider() == BillingProvider.STRIPE)).isTrue();
		assertThat(paidThroughApple.lifecycleState()).isEqualTo(SubscriptionLifecycleState.EXPIRED);
	}

	@Test
	void currentStatusExposesOriginProviderAndManagementChannel() {
		UUID accountId = UUID.randomUUID();
		Subscription apple = rehydrateIndividual(
				accountId,
				BillingProvider.APPLE_APP_STORE,
				SubscriptionLifecycleState.ACTIVE,
				NOW.plusSeconds(3600),
				null);
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(apple));

		IndividualCheckoutService.SubscriptionResult status = checkoutService.currentStatus(accountId);

		assertThat(status.provider()).isEqualTo(BillingProvider.APPLE_APP_STORE);
		assertThat(status.managementChannel()).isEqualTo(IndividualManagementChannel.APPLE_APP_STORE);
	}

	@Test
	void stripePortalRejectedWhenOriginIsApple() {
		UUID accountId = UUID.randomUUID();
		Subscription apple = rehydrateIndividual(
				accountId,
				BillingProvider.APPLE_APP_STORE,
				SubscriptionLifecycleState.ACTIVE,
				NOW.plusSeconds(3600),
				null);
		when(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId))
				.thenReturn(List.of(apple));
		when(customerRepository.findByAccountId(accountId))
				.thenReturn(Optional.of(AccountBillingCustomer.stripe(accountId, "cus_x", NOW)));

		assertThatThrownBy(() -> managementService.openPortal(accountId))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_MANAGED_BY_ORIGIN_PROVIDER"));
		verify(billingProvider, never()).createAccountPortalSession(any(), any());
	}

	@Test
	void orgCapabilitiesAndIndividualPremiumUnionWithoutCrossingSubjects() {
		UUID organizationId = UUID.randomUUID();
		UUID accountId = UUID.randomUUID();
		InMemorySubscriptions store = new InMemorySubscriptions();
		store.save(rehydrateOrg(organizationId, SubscriptionLifecycleState.ACTIVE, NOW.plusSeconds(3600)));
		store.save(rehydrateIndividual(
				accountId,
				BillingProvider.STRIPE,
				SubscriptionLifecycleState.ACTIVE,
				NOW.plusSeconds(3600),
				null));
		EntitlementQueryService query = new EntitlementQueryService(store, CLOCK);

		assertThat(query.capabilities(BillingSubjectType.ORGANIZATION, organizationId))
				.contains(
						CommercialCapability.ORG_TEAM_MANAGEMENT,
						CommercialCapability.ORG_COACH_COLLABORATION,
						CommercialCapability.ORG_COACH_ATHLETE_VIEW,
						CommercialCapability.ORG_TEAM_READINESS)
				.doesNotContain(CommercialCapability.INDIVIDUAL_PREMIUM);
		assertThat(query.capabilities(BillingSubjectType.ACCOUNT, accountId))
				.containsExactly(CommercialCapability.INDIVIDUAL_PREMIUM);
		assertThat(query.hasCapability(
				BillingSubjectType.ORGANIZATION, organizationId, CommercialCapability.INDIVIDUAL_PREMIUM))
				.isFalse();
		assertThat(query.hasCapability(
				BillingSubjectType.ACCOUNT, accountId, CommercialCapability.ORG_TEAM_MANAGEMENT))
				.isFalse();
		assertThat(query.findEffectiveIndividualSubscriptions(accountId)).hasSize(1);
	}

	private void stubCustomer(UUID accountId) {
		AccountBillingCustomer customer = AccountBillingCustomer.stripe(accountId, "cus_test", NOW);
		when(customerRepository.findByAccountId(accountId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByAccountIdForUpdate(accountId)).thenReturn(Optional.of(customer));
	}

	private static Subscription rehydrateIndividual(
			UUID accountId,
			BillingProvider provider,
			SubscriptionLifecycleState state,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt) {
		return Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				provider,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				state,
				provider == BillingProvider.STRIPE ? "cus_test" : accountId.toString(),
				"provider-sub-" + provider.name().toLowerCase(),
				null,
				currentPeriodEndsAt,
				graceEndsAt,
				NOW.minusSeconds(120),
				NOW.minusSeconds(3600),
				NOW.minusSeconds(120),
				1L);
	}

	private static Subscription rehydrateOrg(
			UUID organizationId,
			SubscriptionLifecycleState state,
			Instant currentPeriodEndsAt) {
		return Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				BillingProvider.STRIPE,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.MONTHLY,
				state,
				"cus_org",
				"sub_org",
				null,
				currentPeriodEndsAt,
				null,
				NOW.minusSeconds(120),
				NOW.minusSeconds(3600),
				NOW.minusSeconds(120),
				1L);
	}

	private static final class InMemorySubscriptions implements SubscriptionRepository {

		private final List<Subscription> store = new ArrayList<>();

		@Override
		public Subscription save(Subscription subscription) {
			store.removeIf(existing -> existing.id().equals(subscription.id()));
			store.add(subscription);
			return subscription;
		}

		@Override
		public Optional<Subscription> findById(SubscriptionId id) {
			return store.stream().filter(subscription -> subscription.id().equals(id)).findFirst();
		}

		@Override
		public List<Subscription> findBySubject(BillingSubjectType subjectType, UUID subjectId) {
			return store.stream()
					.filter(subscription -> subscription.subject().type() == subjectType
							&& subscription.subject().subjectId().equals(subjectId))
					.toList();
		}

		@Override
		public Optional<Subscription> findByProviderAndProviderSubscriptionRef(
				BillingProvider provider,
				String providerSubscriptionRef) {
			return Optional.empty();
		}

		@Override
		public List<Subscription> findDueGrace(Instant now, int limit) {
			return List.of();
		}

		@Override
		public List<Subscription> findPastDue(int limit) {
			return List.of();
		}

		@Override
		public List<Subscription> findStalePending(Instant createdAtOrBefore, int limit) {
			return List.of();
		}

		@Override
		public List<Subscription> findElapsedCancelAtPeriodEnd(Instant now, int limit) {
			return List.of();
		}
	}
}
