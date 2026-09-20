package com.devinolabs.uap.billing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;
import com.devinolabs.uap.entitlements.EntitlementPort;
import com.devinolabs.uap.billing.application.EntitlementQueryService;
import com.devinolabs.uap.billing.application.OrganizationBillingCustomerRepository;
import com.devinolabs.uap.billing.application.ProviderEventInbox;
import com.devinolabs.uap.billing.application.ProviderEventReceipt;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderEventProcessingStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, BillingSubscriptionPersistenceIntegrationTests.FixedClockConfig.class })
class BillingSubscriptionPersistenceIntegrationTests {

	private static final Instant T0 = Instant.parse("2026-09-10T12:00:00Z");

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private EntitlementPort entitlementPort;

	@Autowired
	private EntitlementQueryService entitlementQueryService;

	@Autowired
	private OrganizationBillingCustomerRepository customerRepository;

	@Autowired
	private ProviderEventInbox providerEventInbox;

	@Test
	void persistsAndRehydratesSubscriptionRoundTrip() {
		UUID organizationId = UUID.randomUUID();
		Subscription created = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				BillingProvider.STRIPE,
				CommercialPlanKey.ORG_BAND_75,
				Clock.fixed(T0, ZoneOffset.UTC));
		created.beginOrganizationTrial(Clock.fixed(T0, ZoneOffset.UTC));
		created.attachProviderReferences("cus_test", "sub_test_75", Clock.fixed(T0, ZoneOffset.UTC));

		Subscription saved = subscriptionRepository.save(created);
		Subscription loaded = subscriptionRepository.findById(saved.id()).orElseThrow();

		assertThat(loaded.subject().subjectId()).isEqualTo(organizationId);
		assertThat(loaded.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(loaded.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		assertThat(loaded.providerCustomerRef()).isEqualTo("cus_test");
		assertThat(loaded.providerSubscriptionRef()).isEqualTo("sub_test_75");
		assertThat(loaded.organizationBand()).isPresent();
		assertThat(loaded.organizationBand().orElseThrow().maxActiveAthletes()).isEqualTo(75);
	}

	@Test
	void entitlementPortReturnsEmptyWithoutSubscription() {
		UUID accountId = UUID.randomUUID();

		assertThat(entitlementPort.capabilities(BillingSubjectType.ACCOUNT, accountId)).isEmpty();
		assertThat(entitlementPort.hasCapability(
				BillingSubjectType.ACCOUNT, accountId, CommercialCapability.INDIVIDUAL_PREMIUM)).isFalse();
	}

	@Test
	void entitlementPortReflectsActiveIndividualPremium() {
		UUID accountId = UUID.randomUUID();
		Subscription subscription = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.GOOGLE_PLAY,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				Clock.fixed(T0, ZoneOffset.UTC));
		subscription.activate(Instant.parse("2026-10-10T12:00:00Z"), Clock.fixed(T0, ZoneOffset.UTC));
		subscriptionRepository.save(subscription);

		assertThat(entitlementPort.hasCapability(
				BillingSubjectType.ACCOUNT, accountId, CommercialCapability.INDIVIDUAL_PREMIUM)).isTrue();
		assertThat(entitlementPort.capabilities(BillingSubjectType.ACCOUNT, accountId))
				.containsExactly(CommercialCapability.INDIVIDUAL_PREMIUM);
		assertThat(entitlementQueryService.findEffectiveIndividualSubscriptions(accountId)).hasSize(1);
	}

	@Test
	void providerSubscriptionRefIsUniqueWhenPresent() {
		Clock clock = Clock.fixed(T0, ZoneOffset.UTC);
		Subscription first = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				clock);
		first.activate(Instant.parse("2026-10-10T12:00:00Z"), clock);
		first.attachProviderReferences("cus_a", "sub_dup", clock);
		subscriptionRepository.save(first);

		Subscription second = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.randomUUID()),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				clock);
		second.activate(Instant.parse("2026-10-10T12:00:00Z"), clock);
		second.attachProviderReferences("cus_b", "sub_dup", clock);

		assertThatThrownBy(() -> subscriptionRepository.save(second))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void subjectLookupSupportsDuplicateDetectionFoundation() {
		UUID accountId = UUID.randomUUID();
		Clock clock = Clock.fixed(T0, ZoneOffset.UTC);
		Subscription stripe = Subscription.startPending(
				SubscriptionId.generate(),
				BillingSubject.account(accountId),
				BillingProvider.STRIPE,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				clock);
		stripe.activate(Instant.parse("2026-10-10T12:00:00Z"), clock);
		subscriptionRepository.save(stripe);

		assertThat(subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId)).hasSize(1);
		assertThat(entitlementQueryService.findEffectiveIndividualSubscriptions(accountId)).hasSize(1);
	}

	@Test
	void persistsStripeCheckoutCadenceProviderClockAndOrganizationCustomer() {
		UUID organizationId = UUID.randomUUID();
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.generate(),
				BillingSubject.organization(organizationId),
				CommercialPlanKey.ORG_BAND_250,
				BillingCadence.MONTHLY,
				Clock.fixed(T0, ZoneOffset.UTC));
		Instant providerAsOf = T0.plusSeconds(1);
		subscription.synchronizeProviderSnapshot(
				new ProviderSubscriptionSnapshot(
						"cus_org_250",
						"sub_org_250",
						ProviderCommercialStatus.TRIALING,
						false,
						T0.plusSeconds(14 * 24 * 60 * 60),
						T0.plusSeconds(30 * 24 * 60 * 60),
						providerAsOf),
				Clock.fixed(T0, ZoneOffset.UTC));
		customerRepository.save(OrganizationBillingCustomer.stripe(organizationId, "cus_org_250", T0));

		Subscription loaded = subscriptionRepository.findById(subscriptionRepository.save(subscription).id())
				.orElseThrow();

		assertThat(loaded.billingCadence()).isEqualTo(BillingCadence.MONTHLY);
		assertThat(loaded.providerStateAsOf()).isEqualTo(providerAsOf);
		assertThat(customerRepository.findByOrganizationId(organizationId))
				.get()
				.extracting(OrganizationBillingCustomer::providerCustomerRef)
				.isEqualTo("cus_org_250");
	}

	@Test
	void organizationCustomerMappingIsOnePerOrganizationAndProviderReference() {
		UUID organizationId = UUID.randomUUID();
		customerRepository.save(OrganizationBillingCustomer.stripe(organizationId, "cus_unique_org", T0));

		assertThatThrownBy(() -> customerRepository.save(
				OrganizationBillingCustomer.stripe(organizationId, "cus_second_org", T0)))
				.isInstanceOf(DataIntegrityViolationException.class);
		assertThatThrownBy(() -> customerRepository.save(
				OrganizationBillingCustomer.stripe(UUID.randomUUID(), "cus_unique_org", T0)))
				.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void providerEventInboxClaimsOnceAndIgnoresReplayAfterProcessing() {
		Optional<ProviderEventReceipt> first = providerEventInbox.tryBegin(
				BillingProvider.STRIPE, "evt_persist_1", "checkout.session.completed", T0);
		assertThat(first).isPresent();
		providerEventInbox.complete(first.orElseThrow().id(), ProviderEventProcessingStatus.PROCESSED, T0);

		assertThat(providerEventInbox.tryBegin(
				BillingProvider.STRIPE, "evt_persist_1", "checkout.session.completed", T0.plusSeconds(30)))
				.isEmpty();
		assertThat(providerEventInbox.find(BillingProvider.STRIPE, "evt_persist_1"))
				.get()
				.extracting(ProviderEventReceipt::processingStatus)
				.isEqualTo(ProviderEventProcessingStatus.PROCESSED);
	}

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock billingFixedClock() {
			return Clock.fixed(T0, ZoneOffset.UTC);
		}

	}

}
