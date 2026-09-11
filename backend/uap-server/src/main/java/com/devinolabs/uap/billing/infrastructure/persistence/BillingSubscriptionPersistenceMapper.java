package com.devinolabs.uap.billing.infrastructure.persistence;

import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;

final class BillingSubscriptionPersistenceMapper {

	private BillingSubscriptionPersistenceMapper() {
	}

	static BillingSubscriptionJpaEntity toEntity(Subscription subscription, boolean isNew) {
		return new BillingSubscriptionJpaEntity(
				subscription.id().value(),
				subscription.subject().type(),
				subscription.subject().subjectId(),
				subscription.provider(),
				subscription.planKey(),
				subscription.lifecycleState(),
				subscription.providerCustomerRef(),
				subscription.providerSubscriptionRef(),
				subscription.trialEndsAt(),
				subscription.currentPeriodEndsAt(),
				subscription.graceEndsAt(),
				subscription.createdAt(),
				subscription.updatedAt(),
				subscription.version(),
				isNew);
	}

	static Subscription toDomain(BillingSubscriptionJpaEntity entity) {
		return Subscription.rehydrate(
				SubscriptionId.of(entity.getId()),
				new BillingSubject(entity.getSubjectType(), entity.getSubjectId()),
				entity.getProvider(),
				entity.getPlanKey(),
				entity.getLifecycleState(),
				entity.getProviderCustomerRef(),
				entity.getProviderSubscriptionRef(),
				entity.getTrialEndsAt(),
				entity.getCurrentPeriodEndsAt(),
				entity.getGraceEndsAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
