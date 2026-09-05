package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Service
public class ListTeamsForOrganizationUseCase {

	private final TeamRepository teamRepository;
	private final OrganizationAccessGuard accessGuard;

	public ListTeamsForOrganizationUseCase(TeamRepository teamRepository, OrganizationAccessGuard accessGuard) {
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
	}

	@Transactional(readOnly = true)
	public List<TeamResult> execute(AccountId accountId, OrganizationId organizationId) {
		accessGuard.requireActiveMember(accountId, organizationId);
		return teamRepository.findAllByOrganizationId(organizationId).stream()
				.map(TeamResult::from)
				.toList();
	}

}
