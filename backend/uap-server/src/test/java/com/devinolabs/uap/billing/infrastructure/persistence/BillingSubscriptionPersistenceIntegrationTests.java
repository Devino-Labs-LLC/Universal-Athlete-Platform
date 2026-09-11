package com.devinolabs.uap.billing.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.api.CommercialCapability;
import com.devinolabs.uap.billing.api.EntitlementPort;
import com.devinolabs.uap.billing.application.EntitlementQueryService;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
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

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock billingFixedClock() {
			return Clock.fixed(T0, ZoneOffset.UTC);
		}

	}

}
