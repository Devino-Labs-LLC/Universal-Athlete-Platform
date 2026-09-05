package com.devinolabs.uap.organization.infrastructure.persistence;

import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

final class TeamPersistenceMapper {

	private TeamPersistenceMapper() {
	}

	static TeamJpaEntity toEntity(Team team, boolean isNew) {
		return new TeamJpaEntity(
				team.id().value(),
				team.organizationId().value(),
				team.name(),
				team.status(),
				team.createdAt(),
				team.updatedAt(),
				team.version(),
				isNew);
	}

	static Team toDomain(TeamJpaEntity entity) {
		return Team.rehydrate(
				TeamId.of(entity.getId()),
				OrganizationId.of(entity.getOrganizationId()),
				entity.getName(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
