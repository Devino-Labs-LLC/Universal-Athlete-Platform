package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.api.AccountDirectoryPort;
import com.devinolabs.uap.identity.api.AccountDirectoryRef;
import com.devinolabs.uap.identity.api.SecureTokenDigestPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.IssuedInvitation;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;

@Service
public class CreateTeamInvitationUseCase {

	private final OrganizationRepository organizationRepository;
	private final InvitationRepository invitationRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final TeamAccessGuard teamAccessGuard;
	private final AccountDirectoryPort accountDirectoryPort;
	private final SecureTokenDigestPort tokenDigestPort;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public CreateTeamInvitationUseCase(
			OrganizationRepository organizationRepository,
			InvitationRepository invitationRepository,
			TeamMembershipRepository teamMembershipRepository,
			TeamAccessGuard teamAccessGuard,
			AccountDirectoryPort accountDirectoryPort,
			SecureTokenDigestPort tokenDigestPort,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.teamAccessGuard = Objects.requireNonNull(teamAccessGuard);
		this.accountDirectoryPort = Objects.requireNonNull(accountDirectoryPort);
		this.tokenDigestPort = Objects.requireNonNull(tokenDigestPort);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public InvitationResult execute(
			AccountId actorAccountId,
			TeamId teamId,
			String email,
			OrganizationMembershipRole role) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Objects.requireNonNull(role, "role must not be null");

		Team team = teamAccessGuard.requireActiveTeam(actorAccountId, teamId);
		teamAccessGuard.requireInviteCapability(actorAccountId, team, role);

		Organization organization = organizationRepository.findById(team.organizationId())
				.orElseThrow(TeamNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new TeamNotFoundException();
		}

		String normalizedEmail = requireEmail(email);
		AccountDirectoryRef boundAccount = accountDirectoryPort.findByNormalizedEmail(normalizedEmail).orElse(null);
		if (boundAccount != null
				&& teamMembershipRepository.existsActiveMembership(AccountId.of(boundAccount.accountId()), teamId)) {
			throw new MembershipConflictException("MEMBERSHIP_ALREADY_ACTIVE", "Account already has an active team membership");
		}

		IssuedInvitation issued = Invitation.issue(
				InvitationId.generate(),
				team.organizationId(),
				team.id(),
				normalizedEmail,
				boundAccount == null ? null : AccountId.of(boundAccount.accountId()),
				role,
				actorAccountId,
				tokenDigestPort::digest,
				clock);

		try {
			Invitation saved = invitationRepository.save(issued.invitation());
			auditPort.invitationCreated(saved.id(), saved.organizationId(), saved.teamId(), saved.role(), actorAccountId);
			return InvitationResult.from(saved, issued.rawToken());
		}
		catch (DataIntegrityViolationException ex) {
			throw new InvitationConflictException(
					"PENDING_INVITATION_EXISTS",
					"A pending invitation already exists for this email and role");
		}
	}

	private static String requireEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new IllegalArgumentException("email must not be blank");
		}
		return email.trim().toLowerCase(Locale.ROOT);
	}

}
