package com.devinolabs.uap.billing.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;

public interface SubscriptionRepository {

	Subscription save(Subscription subscription);

	Optional<Subscription> findById(SubscriptionId id);

	List<Subscription> findBySubject(BillingSubjectType subjectType, UUID subjectId);

	Optional<Subscription> findByProviderAndProviderSubscriptionRef(
			BillingProvider provider,
			String providerSubscriptionRef);

}
