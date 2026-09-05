package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.organization.application.TeamRepository;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Repository
class JpaTeamRepository implements TeamRepository {

	private final TeamJpaRepository jpaRepository;

	JpaTeamRepository(TeamJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Team save(Team team) {
		Optional<TeamJpaEntity> existing = jpaRepository.findById(team.id().value());
		TeamJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(TeamPersistenceMapper.toEntity(team, true));
		}
		else {
			TeamJpaEntity entity = existing.get();
			entity.applyDomainState(team.name(), team.status(), team.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return TeamPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Team> findById(TeamId id) {
		return jpaRepository.findById(id.value()).map(TeamPersistenceMapper::toDomain);
	}

	@Override
	public List<Team> findAllByOrganizationId(OrganizationId organizationId) {
		return jpaRepository.findAllByOrganizationId(organizationId.value()).stream()
				.map(TeamPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByOrganizationIdAndName(OrganizationId organizationId, String name) {
		return jpaRepository.existsByOrganizationIdAndName(organizationId.value(), name);
	}

}
