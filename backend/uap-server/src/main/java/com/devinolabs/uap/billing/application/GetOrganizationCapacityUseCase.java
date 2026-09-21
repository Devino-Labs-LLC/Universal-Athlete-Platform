package com.devinolabs.uap.billing.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.Ambiguous;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.EffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.NoEffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacityPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

@Service
@Transactional(readOnly = true)
public class GetOrganizationCapacityUseCase {

	private final OrganizationMembershipPort membershipPort;
	private final OrganizationCommercialCapacityPort capacityPort;

	public GetOrganizationCapacityUseCase(
			OrganizationMembershipPort membershipPort,
			OrganizationCommercialCapacityPort capacityPort) {
		this.membershipPort = Objects.requireNonNull(membershipPort, "membershipPort must not be null");
		this.capacityPort = Objects.requireNonNull(capacityPort, "capacityPort must not be null");
	}

	public OrganizationCapacitySnapshot execute(UUID actorAccountId, UUID organizationId) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		if (!membershipPort.canManageOrganization(actorAccountId, organizationId)) {
			throw new BillingOrganizationNotFoundException();
		}
		int activeAthleteCount = Math.toIntExact(membershipPort.countDistinctActiveAthletes(organizationId));
		OrganizationCommercialCapacity capacity = capacityPort.resolve(organizationId);
		return switch (capacity) {
			case EffectiveBand band -> withBand(activeAthleteCount, band.maxActiveAthletes());
			case NoEffectiveBand ignored -> withoutBand(activeAthleteCount);
			case Ambiguous ignored -> withoutBand(activeAthleteCount);
		};
	}

	private static OrganizationCapacitySnapshot withBand(int activeAthleteCount, int bandCapacity) {
		int remaining = Math.max(0, bandCapacity - activeAthleteCount);
		return new OrganizationCapacitySnapshot(
				activeAthleteCount,
				bandCapacity,
				remaining,
				activeAthleteCount == bandCapacity,
				activeAthleteCount > bandCapacity);
	}

	private static OrganizationCapacitySnapshot withoutBand(int activeAthleteCount) {
		return new OrganizationCapacitySnapshot(activeAthleteCount, null, null, false, false);
	}

}
