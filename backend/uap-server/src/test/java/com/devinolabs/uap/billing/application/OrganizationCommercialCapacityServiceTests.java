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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.OrganizationAthleteBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.Ambiguous;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.EffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.NoEffectiveBand;

class OrganizationCommercialCapacityServiceTests {

	private static final Instant T0 = Instant.parse("2026-09-21T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(T0, ZoneOffset.UTC);
	private static final UUID ORGANIZATION_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

	@Test
	void noRowIsNoEffectiveBand() {
		OrganizationCommercialCapacityService service = service(new InMemorySubscriptionRepository(), true);

		assertThat(service.isEnforcementEnabled()).isTrue();
		assertThat(service.resolve(ORGANIZATION_ID)).isInstanceOf(NoEffectiveBand.class);
	}

	@Test
	void enforcementFlagIsIndependentOfResolve() {
		OrganizationCommercialCapacityService disabled = service(new InMemorySubscriptionRepository(), false);
		assertThat(disabled.isEnforcementEnabled()).isFalse();
		assertThat(disabled.resolve(ORGANIZATION_ID)).isInstanceOf(NoEffectiveBand.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("noBandCases")
	void noEffectiveBandLifecycle(
			String unusedName,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt) {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		repository.save(rehydrate(CommercialPlanKey.ORG_BAND_25, state, trialEndsAt, currentPeriodEndsAt, graceEndsAt));
		OrganizationCommercialCapacityService service = service(repository, true);

		assertThat(service.resolve(ORGANIZATION_ID)).isInstanceOf(NoEffectiveBand.class);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("effectiveBandCases")
	void exactlyOneEffectiveBand(
			String unusedName,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			CommercialPlanKey planKey,
			OrganizationAthleteBand expectedBand) {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		repository.save(rehydrate(planKey, state, trialEndsAt, currentPeriodEndsAt, graceEndsAt));
		OrganizationCommercialCapacityService service = service(repository, true);

		assertThat(service.resolve(ORGANIZATION_ID)).isEqualTo(new EffectiveBand(expectedBand));
	}

	@Test
	void twoEffectiveSubscriptionsAreAmbiguousAndDoNotUnionOrPickHighest() {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		repository.save(rehydrate(
				CommercialPlanKey.ORG_BAND_25,
				SubscriptionLifecycleState.ACTIVE,
				null,
				T0.plusSeconds(60),
				null));
		repository.save(rehydrate(
				CommercialPlanKey.ORG_BAND_250,
				SubscriptionLifecycleState.ACTIVE,
				null,
				T0.plusSeconds(60),
				null));
		OrganizationCommercialCapacityService service = service(repository, true);

		assertThat(service.resolve(ORGANIZATION_ID)).isInstanceOf(Ambiguous.class);
	}

	@Test
	void individualPremiumOnAccountDoesNotCreateOrganizationBand() {
		InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
		repository.save(Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.account(UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd")),
				BillingProvider.GOOGLE_PLAY,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				SubscriptionLifecycleState.ACTIVE,
				null,
				null,
				null,
				T0.plusSeconds(60),
				null,
				null,
				T0,
				T0,
				0L));
		OrganizationCommercialCapacityService service = service(repository, true);

		assertThat(service.resolve(ORGANIZATION_ID)).isInstanceOf(NoEffectiveBand.class);
	}

	static Stream<Arguments> noBandCases() {
		return Stream.of(
				Arguments.of("PENDING", SubscriptionLifecycleState.PENDING, null, null, null),
				Arguments.of("PAST_DUE", SubscriptionLifecycleState.PAST_DUE, null, T0.plusSeconds(60), null),
				Arguments.of("EXPIRED", SubscriptionLifecycleState.EXPIRED, null, null, null),
				Arguments.of("TRIALING at trialEndsAt", SubscriptionLifecycleState.TRIALING, T0, null, null),
				Arguments.of("GRACE_PERIOD at graceEndsAt", SubscriptionLifecycleState.GRACE_PERIOD, null, null, T0),
				Arguments.of(
						"CANCEL_AT_PERIOD_END at currentPeriodEndsAt",
						SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
						null,
						T0,
						null));
	}

	static Stream<Arguments> effectiveBandCases() {
		Instant beforeEnd = T0.plusSeconds(60);
		return Stream.of(
				Arguments.of(
						"TRIALING before trialEndsAt BAND_75",
						SubscriptionLifecycleState.TRIALING,
						beforeEnd,
						null,
						null,
						CommercialPlanKey.ORG_BAND_75,
						OrganizationAthleteBand.BAND_75),
				Arguments.of(
						"ACTIVE BAND_25",
						SubscriptionLifecycleState.ACTIVE,
						null,
						beforeEnd,
						null,
						CommercialPlanKey.ORG_BAND_25,
						OrganizationAthleteBand.BAND_25),
				Arguments.of(
						"GRACE_PERIOD before graceEndsAt BAND_250",
						SubscriptionLifecycleState.GRACE_PERIOD,
						null,
						null,
						beforeEnd,
						CommercialPlanKey.ORG_BAND_250,
						OrganizationAthleteBand.BAND_250),
				Arguments.of(
						"CANCEL_AT_PERIOD_END before currentPeriodEndsAt BAND_75",
						SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
						null,
						beforeEnd,
						null,
						CommercialPlanKey.ORG_BAND_75,
						OrganizationAthleteBand.BAND_75));
	}

	private static OrganizationCommercialCapacityService service(
			SubscriptionRepository repository,
			boolean enabled) {
		OrganizationCapacityEnforcementProperties properties = new OrganizationCapacityEnforcementProperties();
		properties.setEnabled(enabled);
		return new OrganizationCommercialCapacityService(properties, repository, CLOCK);
	}

	private static Subscription rehydrate(
			CommercialPlanKey planKey,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt) {
		return Subscription.rehydrate(
				SubscriptionId.generate(),
				BillingSubject.organization(ORGANIZATION_ID),
				BillingProvider.APPLE_APP_STORE,
				planKey,
				BillingCadence.MONTHLY,
				state,
				null,
				null,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				null,
				T0,
				T0,
				0L);
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
