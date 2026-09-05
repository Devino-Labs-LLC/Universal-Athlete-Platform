package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.organization.application.OrganizationRepository;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Repository
class JpaOrganizationRepository implements OrganizationRepository {

	private final OrganizationJpaRepository jpaRepository;

	JpaOrganizationRepository(OrganizationJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Organization save(Organization organization) {
		Optional<OrganizationJpaEntity> existing = jpaRepository.findById(organization.id().value());
		OrganizationJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(OrganizationPersistenceMapper.toEntity(organization, true));
		}
		else {
			OrganizationJpaEntity entity = existing.get();
			entity.applyDomainState(organization.name(), organization.status(), organization.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return OrganizationPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Organization> findById(OrganizationId id) {
		return jpaRepository.findById(id.value()).map(OrganizationPersistenceMapper::toDomain);
	}

	@Override
	public List<Organization> findAllById(Iterable<OrganizationId> ids) {
		List<UUID> uuidIds = StreamSupport.stream(ids.spliterator(), false)
				.map(OrganizationId::value)
				.toList();
		if (uuidIds.isEmpty()) {
			return List.of();
		}
		List<Organization> results = new ArrayList<>();
		for (OrganizationJpaEntity entity : jpaRepository.findAllById(uuidIds)) {
			results.add(OrganizationPersistenceMapper.toDomain(entity));
		}
		return results;
	}

}
