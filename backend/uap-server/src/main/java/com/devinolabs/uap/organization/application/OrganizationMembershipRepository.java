package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;

public interface OrganizationMembershipRepository {

	OrganizationMembership save(OrganizationMembership membership);

	Optional<OrganizationMembership> findById(OrganizationMembershipId id);

	Optional<OrganizationMembership> findActiveByOrganizationIdAndAccountId(
			OrganizationId organizationId,
			AccountId accountId);

	Optional<OrganizationMembership> findActiveOwner(OrganizationId organizationId, AccountId accountId);

	List<OrganizationMembership> findAllActiveByAccountId(AccountId accountId);

	boolean existsActiveMembership(AccountId accountId, OrganizationId organizationId);

	boolean existsActiveOwner(AccountId accountId, OrganizationId organizationId);

}
