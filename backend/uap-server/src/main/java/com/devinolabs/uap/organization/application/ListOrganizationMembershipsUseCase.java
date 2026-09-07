package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Service
public class ListOrganizationMembershipsUseCase {

	private final OrganizationMembershipRepository membershipRepository;
	private final OrganizationAccessGuard accessGuard;

	public ListOrganizationMembershipsUseCase(
			OrganizationMembershipRepository membershipRepository,
			OrganizationAccessGuard accessGuard) {
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
	}

	@Transactional(readOnly = true)
	public List<OrganizationMembershipResult> execute(AccountId accountId, OrganizationId organizationId) {
		accessGuard.requireActiveMember(accountId, organizationId);
		return membershipRepository.findAllByOrganizationId(organizationId).stream()
				.map(OrganizationMembershipResult::from)
				.toList();
	}

}
