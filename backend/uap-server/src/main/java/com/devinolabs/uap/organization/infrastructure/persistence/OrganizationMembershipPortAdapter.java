package com.devinolabs.uap.organization.infrastructure.persistence;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Component
class OrganizationMembershipPortAdapter implements OrganizationMembershipPort {

	private final OrganizationMembershipRepository membershipRepository;

	OrganizationMembershipPortAdapter(OrganizationMembershipRepository membershipRepository) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
	}

	@Override
	public boolean hasActiveOrganizationMembership(UUID accountId, UUID organizationId) {
		return membershipRepository.existsActiveMembership(
				AccountId.of(accountId),
				OrganizationId.of(organizationId));
	}

	@Override
	public boolean canManageOrganization(UUID accountId, UUID organizationId) {
		return membershipRepository.existsActiveOwner(
				AccountId.of(accountId),
				OrganizationId.of(organizationId));
	}

}
