package com.devinolabs.uap.consent.infrastructure.persistence;

import java.util.LinkedHashSet;

import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;

final class ConsentGrantPersistenceMapper {

	private ConsentGrantPersistenceMapper() {
	}

	static ConsentGrantJpaEntity toEntity(ConsentGrant grant, boolean isNew) {
		return new ConsentGrantJpaEntity(
				grant.id().value(),
				grant.athleteId(),
				grant.teamId(),
				grant.organizationId(),
				grant.teamMembershipId(),
				grant.status(),
				grant.createdAt(),
				grant.revokedAt(),
				grant.updatedAt(),
				grant.version(),
				new LinkedHashSet<>(grant.scopes()),
				isNew);
	}

	static ConsentGrant toDomain(ConsentGrantJpaEntity entity) {
		return ConsentGrant.rehydrate(
				ConsentGrantId.of(entity.getId()),
				entity.getAthleteId(),
				entity.getTeamId(),
				entity.getOrganizationId(),
				entity.getTeamMembershipId(),
				new LinkedHashSet<>(entity.getScopes()),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getRevokedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
