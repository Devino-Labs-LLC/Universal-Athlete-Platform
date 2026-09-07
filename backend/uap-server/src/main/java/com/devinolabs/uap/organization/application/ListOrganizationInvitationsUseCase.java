package com.devinolabs.uap.organization.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

@Service
public class ListOrganizationInvitationsUseCase {

	private final InvitationRepository invitationRepository;
	private final OrganizationAccessGuard accessGuard;

	public ListOrganizationInvitationsUseCase(
			InvitationRepository invitationRepository,
			OrganizationAccessGuard accessGuard) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
	}

	@Transactional(readOnly = true)
	public List<InvitationResult> execute(AccountId accountId, OrganizationId organizationId) {
		accessGuard.requireOrgAdminOrOwner(accountId, organizationId);
		return invitationRepository.findAllByOrganizationIdAndTeamIdIsNull(organizationId).stream()
				.map(InvitationResult::from)
				.toList();
	}

}
