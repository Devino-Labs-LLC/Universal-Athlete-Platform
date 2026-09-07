package com.devinolabs.uap.organization.infrastructure.persistence;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

final class TeamMembershipPersistenceMapper {

	private TeamMembershipPersistenceMapper() {
	}

	static TeamMembershipJpaEntity toEntity(TeamMembership membership, boolean isNew) {
		return new TeamMembershipJpaEntity(
				membership.id().value(),
				membership.teamId().value(),
				membership.accountId().value(),
				membership.athleteId(),
				membership.role(),
				membership.status(),
				membership.createdAt(),
				membership.updatedAt(),
				membership.version(),
				isNew);
	}

	static TeamMembership toDomain(TeamMembershipJpaEntity entity) {
		return TeamMembership.rehydrate(
				TeamMembershipId.of(entity.getId()),
				TeamId.of(entity.getTeamId()),
				AccountId.of(entity.getAccountId()),
				entity.getAthleteId(),
				entity.getRole(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
