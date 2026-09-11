package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingProvider;

interface BillingSubscriptionJpaRepository extends JpaRepository<BillingSubscriptionJpaEntity, UUID> {

	List<BillingSubscriptionJpaEntity> findAllBySubjectTypeAndSubjectIdOrderByCreatedAtAsc(
			BillingSubjectType subjectType,
			UUID subjectId);

	Optional<BillingSubscriptionJpaEntity> findByProviderAndProviderSubscriptionRef(
			BillingProvider provider,
			String providerSubscriptionRef);

}
