package com.devinolabs.uap.billing.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.OrganizationAthleteBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.Ambiguous;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.EffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.NoEffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacityPort;

@Service
@Transactional(readOnly = true)
public class OrganizationCommercialCapacityService implements OrganizationCommercialCapacityPort {

	private static final Logger LOG = LoggerFactory.getLogger(OrganizationCommercialCapacityService.class);

	private final OrganizationCapacityEnforcementProperties properties;
	private final SubscriptionRepository subscriptionRepository;
	private final Clock clock;

	public OrganizationCommercialCapacityService(
			OrganizationCapacityEnforcementProperties properties,
			SubscriptionRepository subscriptionRepository,
			Clock clock) {
		this.properties = Objects.requireNonNull(properties, "properties must not be null");
		this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository, "subscriptionRepository must not be null");
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public boolean isEnforcementEnabled() {
		return properties.isEnabled();
	}

	@Override
	public OrganizationCommercialCapacity resolve(UUID organizationId) {
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Instant asOf = Instant.now(clock);
		List<Subscription> effectiveBands = new ArrayList<>();
		for (Subscription subscription : subscriptionRepository.findBySubject(
				BillingSubjectType.ORGANIZATION, organizationId)) {
			if (subscription.isCommerciallyEntitledAt(asOf) && subscription.organizationBand().isPresent()) {
				effectiveBands.add(subscription);
			}
		}
		if (effectiveBands.isEmpty()) {
			return new NoEffectiveBand();
		}
		if (effectiveBands.size() > 1) {
			LOG.warn(
					"Organization commercial capacity is ambiguous: organizationId={} effectiveOrganizationBandCount={}",
					organizationId,
					effectiveBands.size());
			return new Ambiguous();
		}
		return new EffectiveBand(toPublishedBand(effectiveBands.get(0).organizationBand().orElseThrow()));
	}

	private static OrganizationAthleteBand toPublishedBand(
			com.devinolabs.uap.billing.domain.OrganizationAthleteBand band) {
		return switch (band) {
			case BAND_25 -> OrganizationAthleteBand.BAND_25;
			case BAND_75 -> OrganizationAthleteBand.BAND_75;
			case BAND_250 -> OrganizationAthleteBand.BAND_250;
		};
	}

}
