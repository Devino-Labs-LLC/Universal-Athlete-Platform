package com.devinolabs.uap.organization.infrastructure.persistence;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;

final class OrganizationMembershipPersistenceMapper {

	private OrganizationMembershipPersistenceMapper() {
	}

	static OrganizationMembershipJpaEntity toEntity(OrganizationMembership membership, boolean isNew) {
		return new OrganizationMembershipJpaEntity(
				membership.id().value(),
				membership.organizationId().value(),
				membership.accountId().value(),
				membership.athleteId(),
				membership.role(),
				membership.status(),
				membership.createdAt(),
				membership.updatedAt(),
				membership.version(),
				isNew);
	}

	static OrganizationMembership toDomain(OrganizationMembershipJpaEntity entity) {
		return OrganizationMembership.rehydrate(
				OrganizationMembershipId.of(entity.getId()),
				OrganizationId.of(entity.getOrganizationId()),
				AccountId.of(entity.getAccountId()),
				entity.getAthleteId(),
				entity.getRole(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
