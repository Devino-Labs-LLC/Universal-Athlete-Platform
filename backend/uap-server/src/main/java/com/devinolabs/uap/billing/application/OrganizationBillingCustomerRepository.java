package com.devinolabs.uap.billing.application;

import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;

public interface OrganizationBillingCustomerRepository {

	OrganizationBillingCustomer save(OrganizationBillingCustomer customer);

	Optional<OrganizationBillingCustomer> findByOrganizationId(UUID organizationId);

	Optional<OrganizationBillingCustomer> findByOrganizationIdForUpdate(UUID organizationId);

}
