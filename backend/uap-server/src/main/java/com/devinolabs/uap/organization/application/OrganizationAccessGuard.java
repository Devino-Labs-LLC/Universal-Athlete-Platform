package com.devinolabs.uap.organization.application;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;

/**
 * Server-side organization access checks. Denied access throws NotFound-style exceptions (404).
 */
@Service
public class OrganizationAccessGuard {

	private final OrganizationMembershipRepository membershipRepository;

	public OrganizationAccessGuard(OrganizationMembershipRepository membershipRepository) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
	}

	/**
	 * Slice A manage access: ACTIVE membership with ORG_OWNER.
	 */
	public OrganizationMembership requireManageAccess(AccountId accountId, OrganizationId organizationId) {
		return requireActiveOwner(accountId, organizationId);
	}

	public OrganizationMembership requireActiveOwner(AccountId accountId, OrganizationId organizationId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		return membershipRepository.findActiveOwner(organizationId, accountId)
				.orElseThrow(OrganizationNotFoundException::new);
	}

	/**
	 * Any ACTIVE membership may read. Slice A only creates ORG_OWNER, so this matches owner in practice.
	 */
	public OrganizationMembership requireActiveMember(AccountId accountId, OrganizationId organizationId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		return membershipRepository.findActiveByOrganizationIdAndAccountId(organizationId, accountId)
				.orElseThrow(OrganizationNotFoundException::new);
	}

}
