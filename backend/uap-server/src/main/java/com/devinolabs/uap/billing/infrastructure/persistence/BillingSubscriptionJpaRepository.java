package com.devinolabs.uap.billing.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;

interface BillingSubscriptionJpaRepository extends JpaRepository<BillingSubscriptionJpaEntity, UUID> {

	List<BillingSubscriptionJpaEntity> findAllBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(
			BillingSubjectType subjectType,
			UUID subjectId);

	Optional<BillingSubscriptionJpaEntity> findByProviderAndProviderSubscriptionRef(
			BillingProvider provider,
			String providerSubscriptionRef);

	@Query("""
			select subscription from BillingSubscriptionJpaEntity subscription
			where subscription.lifecycleState = com.devinolabs.uap.billing.domain.SubscriptionLifecycleState.GRACE_PERIOD
			and subscription.graceEndsAt <= :now
			order by subscription.graceEndsAt asc, subscription.id asc
			""")
	List<BillingSubscriptionJpaEntity> findDueGrace(@Param("now") Instant now, Pageable pageable);

	List<BillingSubscriptionJpaEntity> findByLifecycleStateOrderByCreatedAtAscIdAsc(
			SubscriptionLifecycleState lifecycleState,
			Pageable pageable);

	@Query("""
			select subscription from BillingSubscriptionJpaEntity subscription
			where subscription.lifecycleState = com.devinolabs.uap.billing.domain.SubscriptionLifecycleState.PENDING
			and subscription.createdAt <= :createdAtOrBefore
			order by subscription.createdAt asc, subscription.id asc
			""")
	List<BillingSubscriptionJpaEntity> findStalePending(
			@Param("createdAtOrBefore") Instant createdAtOrBefore,
			Pageable pageable);

	@Query("""
			select subscription from BillingSubscriptionJpaEntity subscription
			where subscription.lifecycleState = com.devinolabs.uap.billing.domain.SubscriptionLifecycleState.CANCEL_AT_PERIOD_END
			and subscription.currentPeriodEndsAt <= :now
			order by subscription.currentPeriodEndsAt asc, subscription.id asc
			""")
	List<BillingSubscriptionJpaEntity> findElapsedCancelAtPeriodEnd(@Param("now") Instant now, Pageable pageable);

}
