package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;

public interface OrganizationRepository {

	Organization save(Organization organization);

	Optional<Organization> findById(OrganizationId id);

	List<Organization> findAllById(Iterable<OrganizationId> ids);

}
