package com.devinolabs.uap.organization.application;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

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

	/**
	 * ACTIVE ORG_ADMIN or ORG_OWNER may manage org-scoped invitations and memberships.
	 */
	public OrganizationMembership requireOrgAdminOrOwner(AccountId accountId, OrganizationId organizationId) {
		OrganizationMembership membership = requireActiveMember(accountId, organizationId);
		if (membership.role() != OrganizationMembershipRole.ORG_ADMIN
				&& membership.role() != OrganizationMembershipRole.ORG_OWNER) {
			throw new OrganizationNotFoundException();
		}
		return membership;
	}

}
