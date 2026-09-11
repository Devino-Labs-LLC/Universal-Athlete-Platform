package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;

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
					subscription.lifecycleState(),
					subscription.providerCustomerRef(),
					subscription.providerSubscriptionRef(),
					subscription.trialEndsAt(),
					subscription.currentPeriodEndsAt(),
					subscription.graceEndsAt(),
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

}
