package com.devinolabs.uap.organization.application;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamMembership;

@Service
public class ListOrganizationsForAccountUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRepository teamRepository;

	public ListOrganizationsForAccountUseCase(
			OrganizationRepository organizationRepository,
			OrganizationMembershipRepository membershipRepository,
			TeamMembershipRepository teamMembershipRepository,
			TeamRepository teamRepository) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
	}

	@Transactional(readOnly = true)
	public List<OrganizationResult> execute(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Set<OrganizationId> organizationIds = new LinkedHashSet<>();
		for (OrganizationMembership membership : membershipRepository.findAllActiveByAccountId(accountId)) {
			organizationIds.add(membership.organizationId());
		}
		for (TeamMembership teamMembership : teamMembershipRepository.findAllActiveByAccountId(accountId)) {
			teamRepository.findById(teamMembership.teamId())
					.map(Team::organizationId)
					.ifPresent(organizationIds::add);
		}
		return organizationRepository.findAllById(List.copyOf(organizationIds)).stream()
				.map(OrganizationResult::from)
				.toList();
	}

}
