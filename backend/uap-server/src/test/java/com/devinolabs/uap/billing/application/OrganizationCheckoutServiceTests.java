package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

class OrganizationCheckoutServiceTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	private OrganizationMembershipPort membershipPort;
	private OrganizationBillingCustomerRepository customerRepository;
	private SubscriptionRepository subscriptionRepository;
	private OrganizationBillingProvider billingProvider;
	private BillingAuditPort auditPort;
	private OrganizationCheckoutService service;

	@BeforeEach
	void setUp() {
		membershipPort = mock(OrganizationMembershipPort.class);
		customerRepository = mock(OrganizationBillingCustomerRepository.class);
		subscriptionRepository = mock(SubscriptionRepository.class);
		billingProvider = mock(OrganizationBillingProvider.class);
		auditPort = mock(BillingAuditPort.class);
		service = new OrganizationCheckoutService(
				membershipPort,
				customerRepository,
				subscriptionRepository,
				billingProvider,
				auditPort,
				CLOCK);
	}

	@Test
	void ownerStartsServerPricedIdempotentCheckout() {
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		OrganizationBillingCustomer customer = customer(organizationId);
		when(membershipPort.canManageOrganization(actorId, organizationId)).thenReturn(true);
		when(customerRepository.findByOrganizationId(organizationId)).thenReturn(Optional.empty());
		when(billingProvider.createCustomer(organizationId)).thenReturn("cus_test_org");
		when(customerRepository.save(any())).thenReturn(customer);
		when(customerRepository.findByOrganizationIdForUpdate(organizationId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ORGANIZATION, organizationId))
				.thenReturn(List.of());
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(billingProvider.createCheckoutSession(
				organizationId,
				requestId,
				"cus_test_org",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.thenReturn(new OrganizationBillingProvider.CheckoutSession(
						"cs_test_checkout", "https://checkout.stripe.test/session"));

		OrganizationCheckoutService.CheckoutResult result = service.startCheckout(
				actorId,
				organizationId,
				requestId,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL);

		assertThat(result.subscriptionId()).isEqualTo(requestId);
		assertThat(result.checkoutSessionId()).isEqualTo("cs_test_checkout");
		verify(auditPort).checkoutInitiated(
				requestId, organizationId, actorId, CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL);
	}

	@Test
	void repeatedRequestReusesPendingIdentityWithoutDuplicateAudit() {
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		OrganizationBillingCustomer customer = customer(organizationId);
		Subscription pending = pending(organizationId, requestId);
		when(membershipPort.canManageOrganization(actorId, organizationId)).thenReturn(true);
		when(customerRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByOrganizationIdForUpdate(organizationId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(requestId))).thenReturn(Optional.of(pending));
		when(billingProvider.createCheckoutSession(any(), any(), any(), any(), any()))
				.thenReturn(new OrganizationBillingProvider.CheckoutSession(
						"cs_test_same", "https://checkout.stripe.test/same"));

		service.startCheckout(
				actorId,
				organizationId,
				requestId,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL);
		service.startCheckout(
				actorId,
				organizationId,
				requestId,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL);

		verify(billingProvider, times(2)).createCheckoutSession(any(), any(), any(), any(), any());
		verify(subscriptionRepository, never()).save(any());
		verify(auditPort, never()).checkoutInitiated(any(), any(), any(), any(), any());
	}

	@Test
	void nonOwnerFailsClosedBeforeCustomerOrCheckoutAccess() {
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		when(membershipPort.canManageOrganization(actorId, organizationId)).thenReturn(false);

		assertThatThrownBy(() -> service.startCheckout(
				actorId,
				organizationId,
				UUID.randomUUID(),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY))
				.isInstanceOf(BillingOrganizationNotFoundException.class);
		verify(customerRepository, never()).findByOrganizationId(any());
		verify(billingProvider, never()).createCustomer(any());
	}

	@Test
	void anotherOpenCommercialRelationshipBlocksDuplicateCheckout() {
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		OrganizationBillingCustomer customer = customer(organizationId);
		Subscription existing = pending(organizationId, UUID.randomUUID());
		when(membershipPort.canManageOrganization(actorId, organizationId)).thenReturn(true);
		when(customerRepository.findByOrganizationId(organizationId)).thenReturn(Optional.of(customer));
		when(customerRepository.findByOrganizationIdForUpdate(organizationId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(any())).thenReturn(Optional.empty());
		when(subscriptionRepository.findBySubject(BillingSubjectType.ORGANIZATION, organizationId))
				.thenReturn(List.of(existing));

		assertThatThrownBy(() -> service.startCheckout(
				actorId,
				organizationId,
				UUID.randomUUID(),
				CommercialPlanKey.ORG_BAND_25,
				BillingCadence.MONTHLY))
				.isInstanceOfSatisfying(BillingConflictException.class,
						ex -> assertThat(ex.code()).isEqualTo("BILLING_CHECKOUT_IN_PROGRESS"));
		verify(billingProvider, never()).createCheckoutSession(any(), any(), any(), any(), any());
	}

	@Test
	void explicitSyncUsesAuthoritativeSnapshotAndEqualReplayIsIdempotent() {
		UUID actorId = UUID.randomUUID();
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		OrganizationBillingCustomer customer = customer(organizationId);
		Subscription pending = pending(organizationId, subscriptionId);
		ProviderSubscriptionSnapshot snapshot = new ProviderSubscriptionSnapshot(
				"cus_test_org",
				"sub_test_org",
				ProviderCommercialStatus.TRIALING,
				false,
				NOW.plusSeconds(14 * 24 * 60 * 60),
				NOW.plusSeconds(30 * 24 * 60 * 60),
				NOW.plusSeconds(1));
		when(membershipPort.canManageOrganization(actorId, organizationId)).thenReturn(true);
		when(customerRepository.findByOrganizationIdForUpdate(organizationId)).thenReturn(Optional.of(customer));
		when(subscriptionRepository.findById(SubscriptionId.of(subscriptionId))).thenReturn(Optional.of(pending));
		when(billingProvider.fetchCheckoutSubscription(any(), any(), any(), any(), any(), any()))
				.thenReturn(snapshot);
		when(subscriptionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		OrganizationCheckoutService.SubscriptionResult first = service.synchronize(
				actorId, organizationId, subscriptionId, "cs_test_sync");
		OrganizationCheckoutService.SubscriptionResult replay = service.synchronize(
				actorId, organizationId, subscriptionId, "cs_test_sync");

		assertThat(first.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(replay.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		verify(subscriptionRepository).save(pending);
		verify(auditPort).subscriptionSynchronized(
				subscriptionId, organizationId, actorId, SubscriptionLifecycleState.TRIALING);
	}

	private static OrganizationBillingCustomer customer(UUID organizationId) {
		return OrganizationBillingCustomer.stripe(organizationId, "cus_test_org", NOW);
	}

	private static Subscription pending(UUID organizationId, UUID subscriptionId) {
		return Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL,
				CLOCK);
	}

}
