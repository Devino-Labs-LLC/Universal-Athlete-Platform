package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;

@Service
public class ListOrganizationsForAccountUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;

	public ListOrganizationsForAccountUseCase(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
	}

	@Transactional(readOnly = true)
	public List<OrganizationResult> execute(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		List<OrganizationId> organizationIds = membershipRepository.findAllActiveByAccountId(accountId).stream()
				.map(OrganizationMembership::organizationId)
				.distinct()
				.toList();
		return organizationRepository.findAllById(organizationIds).stream()
				.map(OrganizationResult::from)
				.toList();
	}

}
