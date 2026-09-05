package com.devinolabs.uap.organization.infrastructure.persistence;

import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;

final class OrganizationPersistenceMapper {

	private OrganizationPersistenceMapper() {
	}

	static OrganizationJpaEntity toEntity(Organization organization, boolean isNew) {
		return new OrganizationJpaEntity(
				organization.id().value(),
				organization.name(),
				organization.status(),
				organization.createdAt(),
				organization.updatedAt(),
				organization.version(),
				isNew);
	}

	static Organization toDomain(OrganizationJpaEntity entity) {
		return Organization.rehydrate(
				OrganizationId.of(entity.getId()),
				entity.getName(),
				entity.getStatus(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
