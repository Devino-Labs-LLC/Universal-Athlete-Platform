package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

@Repository
class JpaSubscriptionRepository implements SubscriptionRepository {

	private final BillingSubscriptionJpaRepository jpaRepository;

	JpaSubscriptionRepository(BillingSubscriptionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Subscription save(Subscription subscription) {
		Optional<BillingSubscriptionJpaEntity> existing = jpaRepository.findById(subscription.id().value());
		BillingSubscriptionJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(BillingSubscriptionPersistenceMapper.toEntity(subscription, true));
		}
		else {
			BillingSubscriptionJpaEntity entity = existing.get();
			entity.applyDomainState(
					subscription.planKey(),
					subscription.billingCadence(),
					subscription.lifecycleState(),
					subscription.providerCustomerRef(),
					subscription.providerSubscriptionRef(),
					subscription.trialEndsAt(),
					subscription.currentPeriodEndsAt(),
					subscription.graceEndsAt(),
					subscription.providerStateAsOf(),
					subscription.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return BillingSubscriptionPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Subscription> findById(SubscriptionId id) {
		return jpaRepository.findById(id.value()).map(BillingSubscriptionPersistenceMapper::toDomain);
	}

	@Override
	public List<Subscription> findBySubject(BillingSubjectType subjectType, UUID subjectId) {
		return jpaRepository.findAllBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(subjectType, subjectId).stream()
				.map(BillingSubscriptionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<Subscription> findByProviderAndProviderSubscriptionRef(
			BillingProvider provider,
			String providerSubscriptionRef) {
		return jpaRepository.findByProviderAndProviderSubscriptionRef(provider, providerSubscriptionRef)
				.map(BillingSubscriptionPersistenceMapper::toDomain);
	}

	@Override
	public List<Subscription> findDueGrace(Instant now, int limit) {
		return mapPage(jpaRepository.findDueGrace(now, page(limit)));
	}

	@Override
	public List<Subscription> findPastDue(int limit) {
		return mapPage(jpaRepository.findByLifecycleStateOrderByCreatedAtAscIdAsc(
				SubscriptionLifecycleState.PAST_DUE, page(limit)));
	}

	@Override
	public List<Subscription> findStalePending(Instant createdAtOrBefore, int limit) {
		return mapPage(jpaRepository.findStalePending(createdAtOrBefore, page(limit)));
	}

	@Override
	public List<Subscription> findElapsedCancelAtPeriodEnd(Instant now, int limit) {
		return mapPage(jpaRepository.findElapsedCancelAtPeriodEnd(now, page(limit)));
	}

	private static PageRequest page(int limit) {
		return PageRequest.of(0, limit);
	}

	private static List<Subscription> mapPage(List<BillingSubscriptionJpaEntity> entities) {
		return entities.stream().map(BillingSubscriptionPersistenceMapper::toDomain).toList();
	}

}
