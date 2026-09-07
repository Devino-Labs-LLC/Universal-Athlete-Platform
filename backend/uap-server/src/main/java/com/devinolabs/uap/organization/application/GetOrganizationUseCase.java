package com.devinolabs.uap.organization.application;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Service
public class GetOrganizationUseCase {

	private final OrganizationRepository organizationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamRepository teamRepository;

	public GetOrganizationUseCase(
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
	public OrganizationResult execute(AccountId accountId, OrganizationId organizationId) {
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (membershipRepository.existsActiveMembership(accountId, organizationId)) {
			return OrganizationResult.from(organization);
		}
		boolean viaTeam = teamMembershipRepository.findAllActiveByAccountId(accountId).stream()
				.map(membership -> teamRepository.findById(membership.teamId()))
				.flatMap(Optional::stream)
				.anyMatch(team -> team.organizationId().equals(organizationId));
		if (!viaTeam) {
			throw new OrganizationNotFoundException();
		}
		return OrganizationResult.from(organization);
	}

}
