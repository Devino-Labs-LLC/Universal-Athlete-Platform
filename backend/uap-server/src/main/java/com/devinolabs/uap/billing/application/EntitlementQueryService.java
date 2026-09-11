package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.api.BillingSubjectType;
import com.devinolabs.uap.billing.api.CommercialCapability;
import com.devinolabs.uap.billing.api.EntitlementPort;
import com.devinolabs.uap.billing.domain.Subscription;

/**
 * Published entitlement lookup. Does not enforce product edges (Slice C).
 */
@Service
@Transactional(readOnly = true)
public class EntitlementQueryService implements EntitlementPort {

	private final SubscriptionRepository subscriptionRepository;
	private final Clock clock;

	public EntitlementQueryService(SubscriptionRepository subscriptionRepository, Clock clock) {
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Override
	public boolean hasCapability(BillingSubjectType subjectType, UUID subjectId, CommercialCapability capability) {
		Objects.requireNonNull(subjectType, "subjectType must not be null");
		Objects.requireNonNull(subjectId, "subjectId must not be null");
		Objects.requireNonNull(capability, "capability must not be null");
		return capabilities(subjectType, subjectId).contains(capability);
	}

	@Override
	public Set<CommercialCapability> capabilities(BillingSubjectType subjectType, UUID subjectId) {
		Objects.requireNonNull(subjectType, "subjectType must not be null");
		Objects.requireNonNull(subjectId, "subjectId must not be null");
		Instant asOf = Instant.now(clock);
		List<Subscription> subscriptions = subscriptionRepository.findBySubject(subjectType, subjectId);
		if (subscriptions.isEmpty()) {
			return Set.of();
		}
		EnumSet<CommercialCapability> granted = EnumSet.noneOf(CommercialCapability.class);
		for (Subscription subscription : subscriptions) {
			granted.addAll(subscription.effectiveCapabilitiesAt(asOf));
		}
		return granted.isEmpty() ? Set.of() : Set.copyOf(granted);
	}

	/**
	 * Foundation for ADR-045: list currently entitled individual Account subscriptions.
	 */
	public List<Subscription> findEffectiveIndividualSubscriptions(UUID accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Instant asOf = Instant.now(clock);
		return subscriptionRepository.findBySubject(BillingSubjectType.ACCOUNT, accountId).stream()
				.filter(subscription -> subscription.isEffectiveIndividualRelationshipAt(asOf))
				.toList();
	}

}
