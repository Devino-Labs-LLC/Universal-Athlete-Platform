package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.api.AccountDirectoryPort;
import com.devinolabs.uap.identity.api.AccountDirectoryRef;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.Team;

@Service
public class ListMyInvitationsUseCase {

	private final InvitationRepository invitationRepository;
	private final OrganizationRepository organizationRepository;
	private final TeamRepository teamRepository;
	private final AccountDirectoryPort accountDirectoryPort;
	private final Clock clock;

	public ListMyInvitationsUseCase(
			InvitationRepository invitationRepository,
			OrganizationRepository organizationRepository,
			TeamRepository teamRepository,
			AccountDirectoryPort accountDirectoryPort,
			Clock clock) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.accountDirectoryPort = Objects.requireNonNull(accountDirectoryPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public List<MyInvitationResult> execute(AccountId accountId) {
		AccountDirectoryRef account = accountDirectoryPort.findByAccountId(accountId.value())
				.orElseThrow(InvitationNotFoundException::new);
		return invitationRepository.findPendingByInvitedEmail(account.normalizedEmail()).stream()
				.filter(invitation -> invitation.isEffective(clock))
				.map(this::toResult)
				.collect(Collectors.toList());
	}

	private MyInvitationResult toResult(Invitation invitation) {
		Organization organization = organizationRepository.findById(invitation.organizationId())
				.orElseThrow(InvitationNotFoundException::new);
		String teamName = null;
		if (invitation.teamId() != null) {
			Team team = teamRepository.findById(invitation.teamId()).orElseThrow(InvitationNotFoundException::new);
			teamName = team.name();
		}
		return new MyInvitationResult(
				invitation.id().value(),
				organization.id().value(),
				organization.name(),
				invitation.teamId() == null ? null : invitation.teamId().value(),
				teamName,
				invitation.role(),
				invitation.expiresAt());
	}

}
