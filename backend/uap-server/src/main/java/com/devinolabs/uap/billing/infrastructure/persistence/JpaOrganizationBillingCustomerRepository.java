package com.devinolabs.uap.billing.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.billing.application.OrganizationBillingCustomerRepository;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;

@Repository
class JpaOrganizationBillingCustomerRepository implements OrganizationBillingCustomerRepository {

	private final OrganizationBillingCustomerJpaRepository jpaRepository;

	JpaOrganizationBillingCustomerRepository(OrganizationBillingCustomerJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public OrganizationBillingCustomer save(OrganizationBillingCustomer customer) {
		OrganizationBillingCustomerJpaEntity entity = new OrganizationBillingCustomerJpaEntity(
				customer.organizationId(),
				customer.provider(),
				customer.providerCustomerRef(),
				customer.createdAt(),
				customer.updatedAt());
		return toDomain(jpaRepository.saveAndFlush(entity));
	}

	@Override
	public Optional<OrganizationBillingCustomer> findByOrganizationId(UUID organizationId) {
		return jpaRepository.findById(organizationId).map(JpaOrganizationBillingCustomerRepository::toDomain);
	}

	@Override
	public Optional<OrganizationBillingCustomer> findByOrganizationIdForUpdate(UUID organizationId) {
		return jpaRepository.findByOrganizationIdForUpdate(organizationId)
				.map(JpaOrganizationBillingCustomerRepository::toDomain);
	}

	private static OrganizationBillingCustomer toDomain(OrganizationBillingCustomerJpaEntity entity) {
		return new OrganizationBillingCustomer(
				entity.getId(),
				entity.provider(),
				entity.providerCustomerRef(),
				entity.createdAt(),
				entity.updatedAt());
	}

}
