package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.billing.support.OrganizationSubscriptionFixtures;

class EntitlementQueryLifecycleTests {

	private static final Instant T0 = Instant.parse("2026-09-20T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
	private static final UUID ORGANIZATION_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

	@ParameterizedTest(name = "{0}")
	@MethodSource("lifecycleCases")
	void organizationCapabilitiesFollowSubscriptionTemporalEntitlement(
			String unusedName,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			boolean entitled) {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		repository.save(Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.organization(ORGANIZATION_ID),
				BillingProvider.APPLE_APP_STORE,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL,
				state,
				null,
				null,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				null,
				T0,
				T0,
				0L));
		EntitlementQueryService query = new EntitlementQueryService(repository, CLOCK);

		assertThat(query.hasCapability(
				BillingSubjectType.ORGANIZATION,
				ORGANIZATION_ID,
				CommercialCapability.ORG_TEAM_MANAGEMENT)).isEqualTo(entitled);
		assertThat(query.hasCapability(
				BillingSubjectType.ORGANIZATION,
				ORGANIZATION_ID,
				CommercialCapability.ORG_COACH_COLLABORATION)).isEqualTo(entitled);
		assertThat(query.hasCapability(
				BillingSubjectType.ORGANIZATION,
				ORGANIZATION_ID,
				CommercialCapability.INDIVIDUAL_PREMIUM)).isFalse();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("lifecycleCases")
	void guardUsesPortTemporalResultWhenEnforcementEnabled(
			String unusedName,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			boolean entitled) {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		OrganizationSubscriptionFixtures.saveOrganizationState(
				repository,
				ORGANIZATION_ID,
				state,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				T0);
		EntitlementEnforcementProperties properties = new EntitlementEnforcementProperties();
		properties.setEnabled(true);
		CommercialEntitlementGuardService guard = new CommercialEntitlementGuardService(
				properties,
				new EntitlementQueryService(repository, CLOCK));

		if (entitled) {
			guard.requireOrganizationCapability(ORGANIZATION_ID, CommercialCapability.ORG_TEAM_READINESS);
		}
		else {
			org.assertj.core.api.Assertions.assertThatThrownBy(() -> guard.requireOrganizationCapability(
					ORGANIZATION_ID, CommercialCapability.ORG_TEAM_READINESS))
					.isInstanceOf(com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException.class);
		}
	}

	static Stream<Arguments> lifecycleCases() {
		Instant beforeEnd = T0.plusSeconds(60);
		return Stream.of(
				Arguments.of("PENDING", SubscriptionLifecycleState.PENDING, null, null, null, false),
				Arguments.of("ACTIVE", SubscriptionLifecycleState.ACTIVE, null, beforeEnd, null, true),
				Arguments.of("EXPIRED", SubscriptionLifecycleState.EXPIRED, null, null, null, false),
				Arguments.of("PAST_DUE", SubscriptionLifecycleState.PAST_DUE, null, beforeEnd, null, false),
				Arguments.of("TRIALING before trialEndsAt", SubscriptionLifecycleState.TRIALING, beforeEnd, null, null, true),
				Arguments.of("TRIALING at trialEndsAt", SubscriptionLifecycleState.TRIALING, T0, null, null, false),
				Arguments.of(
						"CANCEL_AT_PERIOD_END before currentPeriodEndsAt",
						SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
						null,
						beforeEnd,
						null,
						true),
				Arguments.of(
						"CANCEL_AT_PERIOD_END at currentPeriodEndsAt",
						SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
						null,
						T0,
						null,
						false),
				Arguments.of("GRACE_PERIOD before graceEndsAt", SubscriptionLifecycleState.GRACE_PERIOD, null, null, beforeEnd, true),
				Arguments.of("GRACE_PERIOD at graceEndsAt", SubscriptionLifecycleState.GRACE_PERIOD, null, null, T0, false));
	}

	private static final class InMemorySubscriptionRepository implements SubscriptionRepository {

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
	}
}
