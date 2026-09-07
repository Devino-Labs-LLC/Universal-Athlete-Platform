package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.identity.api.AccountDirectoryPort;
import com.devinolabs.uap.identity.api.AccountDirectoryRef;
import com.devinolabs.uap.identity.api.SecureTokenDigestPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.InvitationStatus;
import com.devinolabs.uap.organization.domain.Organization;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationStatus;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.domain.TeamStatus;

@Service
public class AcceptInvitationUseCase {

	private final InvitationRepository invitationRepository;
	private final OrganizationRepository organizationRepository;
	private final TeamRepository teamRepository;
	private final OrganizationMembershipRepository organizationMembershipRepository;
	private final TeamMembershipRepository teamMembershipRepository;
	private final AccountDirectoryPort accountDirectoryPort;
	private final SecureTokenDigestPort tokenDigestPort;
	private final AthleteContextPort athleteContextPort;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public AcceptInvitationUseCase(
			InvitationRepository invitationRepository,
			OrganizationRepository organizationRepository,
			TeamRepository teamRepository,
			OrganizationMembershipRepository organizationMembershipRepository,
			TeamMembershipRepository teamMembershipRepository,
			AccountDirectoryPort accountDirectoryPort,
			SecureTokenDigestPort tokenDigestPort,
			AthleteContextPort athleteContextPort,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.teamRepository = Objects.requireNonNull(teamRepository);
		this.organizationMembershipRepository = Objects.requireNonNull(organizationMembershipRepository);
		this.teamMembershipRepository = Objects.requireNonNull(teamMembershipRepository);
		this.accountDirectoryPort = Objects.requireNonNull(accountDirectoryPort);
		this.tokenDigestPort = Objects.requireNonNull(tokenDigestPort);
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public MembershipAcceptResult executeByRawToken(AccountId acceptingAccountId, String rawToken) {
		Objects.requireNonNull(acceptingAccountId, "acceptingAccountId must not be null");
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvitationNotFoundException();
		}
		String tokenHash = tokenDigestPort.digest(rawToken);
		Invitation invitation = invitationRepository.findByTokenHashForUpdate(tokenHash)
				.orElseThrow(InvitationNotFoundException::new);
		return accept(acceptingAccountId, invitation);
	}

	@Transactional
	public MembershipAcceptResult executeByInvitationId(AccountId acceptingAccountId, InvitationId invitationId) {
		Objects.requireNonNull(acceptingAccountId, "acceptingAccountId must not be null");
		Objects.requireNonNull(invitationId, "invitationId must not be null");
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(InvitationNotFoundException::new);
		return accept(acceptingAccountId, invitation);
	}

	private MembershipAcceptResult accept(AccountId acceptingAccountId, Invitation invitation) {
		AccountDirectoryRef account = accountDirectoryPort.findByAccountId(acceptingAccountId.value())
				.orElseThrow(InvitationNotFoundException::new);

		if (invitation.status() == InvitationStatus.ACCEPTED) {
			return resolveIdempotentAccept(acceptingAccountId, invitation);
		}

		if (invitation.status() != InvitationStatus.PENDING) {
			throw new InvitationNotFoundException();
		}

		if (invitation.isExpired(clock)) {
			invitation.markExpired(clock);
			invitationRepository.save(invitation);
			throw new InvitationNotFoundException();
		}

		String normalizedAccountEmail = account.normalizedEmail().trim().toLowerCase(Locale.ROOT);
		if (!invitation.invitedEmail().equals(normalizedAccountEmail)) {
			throw new InvitationNotFoundException();
		}
		if (!account.emailVerified()) {
			throw new InvitationConflictException("EMAIL_UNVERIFIED", "Email must be verified to accept invitation");
		}

		Organization organization = organizationRepository.findById(invitation.organizationId())
				.orElseThrow(InvitationNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new InvitationNotFoundException();
		}

		Team team = null;
		if (invitation.teamId() != null) {
			team = teamRepository.findById(invitation.teamId()).orElseThrow(InvitationNotFoundException::new);
			if (team.status() == TeamStatus.ARCHIVED) {
				throw new InvitationNotFoundException();
			}
			if (!team.organizationId().equals(invitation.organizationId())) {
				throw new InvitationNotFoundException();
			}
		}

		if (invitation.isOrgScoped()) {
			Optional<OrganizationMembership> existing = organizationMembershipRepository
					.findActiveByOrganizationIdAndAccountId(invitation.organizationId(), acceptingAccountId);
			if (existing.isPresent()) {
				throw new MembershipConflictException("MEMBERSHIP_ALREADY_ACTIVE", "Active organization membership already exists");
			}
			return acceptOrgInvitation(acceptingAccountId, invitation, organization);
		}
		Optional<TeamMembership> existingTeam = teamMembershipRepository
				.findActiveByTeamIdAndAccountId(invitation.teamId(), acceptingAccountId);
		if (existingTeam.isPresent()) {
			throw new MembershipConflictException("MEMBERSHIP_ALREADY_ACTIVE", "Active team membership already exists");
		}
		return acceptTeamInvitation(acceptingAccountId, invitation, organization, team);
	}

	private MembershipAcceptResult acceptOrgInvitation(
			AccountId acceptingAccountId,
			Invitation invitation,
			Organization organization) {
		OrganizationMembershipId membershipId = OrganizationMembershipId.generate();
		OrganizationMembership membership = OrganizationMembership.register(
				membershipId,
				invitation.organizationId(),
				acceptingAccountId,
				null,
				invitation.role(),
				clock);

		int updated = invitationRepository.acceptIfPending(
				invitation.id(),
				membershipId.value(),
				java.time.Instant.now(clock));
		if (updated != 1) {
			return recoverConcurrentAccept(acceptingAccountId, invitation.id());
		}

		try {
			organizationMembershipRepository.save(membership);
		}
		catch (DataIntegrityViolationException ex) {
			return recoverConcurrentAccept(acceptingAccountId, invitation.id());
		}

		auditPort.invitationAccepted(invitation.id(), organization.id(), acceptingAccountId);
		auditPort.membershipActivated(
				membershipId.value(),
				organization.id(),
				null,
				membership.role(),
				acceptingAccountId);
		return MembershipAcceptResult.organization(OrganizationMembershipResult.from(membership));
	}

	private MembershipAcceptResult acceptTeamInvitation(
			AccountId acceptingAccountId,
			Invitation invitation,
			Organization organization,
			Team team) {
		UUID athleteId = resolveAthleteId(acceptingAccountId, invitation.role());
		TeamMembershipId membershipId = TeamMembershipId.generate();
		TeamMembership membership = TeamMembership.register(
				membershipId,
				invitation.teamId(),
				acceptingAccountId,
				athleteId,
				invitation.role(),
				clock);

		int updated = invitationRepository.acceptIfPending(
				invitation.id(),
				membershipId.value(),
				java.time.Instant.now(clock));
		if (updated != 1) {
			return recoverConcurrentAccept(acceptingAccountId, invitation.id());
		}

		try {
			teamMembershipRepository.save(membership);
		}
		catch (DataIntegrityViolationException ex) {
			return recoverConcurrentAccept(acceptingAccountId, invitation.id());
		}

		auditPort.invitationAccepted(invitation.id(), organization.id(), acceptingAccountId);
		auditPort.membershipActivated(
				membershipId.value(),
				organization.id(),
				team.id(),
				membership.role(),
				acceptingAccountId);
		return MembershipAcceptResult.team(TeamMembershipResult.from(membership));
	}

	private UUID resolveAthleteId(AccountId accountId, OrganizationMembershipRole role) {
		if (role != OrganizationMembershipRole.ATHLETE) {
			return null;
		}
		try {
			AthleteRef athlete = athleteContextPort.requireAthlete(accountId.value());
			return athlete.athleteId();
		}
		catch (AthleteNotFoundException ex) {
			throw new InvitationConflictException(
					"ATHLETE_PROFILE_REQUIRED",
					"Athlete profile is required to accept an athlete invitation");
		}
	}

	private MembershipAcceptResult resolveIdempotentAccept(AccountId acceptingAccountId, Invitation invitation) {
		if (invitation.acceptedMembershipId() == null) {
			throw new InvitationNotFoundException();
		}
		if (invitation.isOrgScoped()) {
			OrganizationMembership membership = organizationMembershipRepository
					.findById(OrganizationMembershipId.of(invitation.acceptedMembershipId()))
					.orElseThrow(InvitationNotFoundException::new);
			if (!membership.accountId().equals(acceptingAccountId)) {
				throw new InvitationNotFoundException();
			}
			return MembershipAcceptResult.organization(OrganizationMembershipResult.from(membership));
		}
		TeamMembership membership = teamMembershipRepository
				.findById(TeamMembershipId.of(invitation.acceptedMembershipId()))
				.orElseThrow(InvitationNotFoundException::new);
		if (!membership.accountId().equals(acceptingAccountId)) {
			throw new InvitationNotFoundException();
		}
		return MembershipAcceptResult.team(TeamMembershipResult.from(membership));
	}

	private MembershipAcceptResult recoverConcurrentAccept(AccountId acceptingAccountId, InvitationId invitationId) {
		Invitation reloaded = invitationRepository.findById(invitationId)
				.orElseThrow(InvitationNotFoundException::new);
		if (reloaded.status() == InvitationStatus.ACCEPTED) {
			return resolveIdempotentAccept(acceptingAccountId, reloaded);
		}
		if (reloaded.teamId() != null) {
			Optional<TeamMembership> membership = teamMembershipRepository
					.findActiveByTeamIdAndAccountId(reloaded.teamId(), acceptingAccountId);
			if (membership.isPresent() && reloaded.status() == InvitationStatus.ACCEPTED) {
				return MembershipAcceptResult.team(TeamMembershipResult.from(membership.get()));
			}
		}
		else {
			Optional<OrganizationMembership> membership = organizationMembershipRepository
					.findActiveByOrganizationIdAndAccountId(reloaded.organizationId(), acceptingAccountId);
			if (membership.isPresent() && reloaded.status() == InvitationStatus.ACCEPTED) {
				return MembershipAcceptResult.organization(OrganizationMembershipResult.from(membership.get()));
			}
		}
		throw new InvitationNotFoundException();
	}

	public record MembershipAcceptResult(
			OrganizationMembershipResult organizationMembership,
			TeamMembershipResult teamMembership) {

		public static MembershipAcceptResult organization(OrganizationMembershipResult membership) {
			return new MembershipAcceptResult(membership, null);
		}

		public static MembershipAcceptResult team(TeamMembershipResult membership) {
			return new MembershipAcceptResult(null, membership);
		}

		public boolean isTeam() {
			return teamMembership != null;
		}

	}

}
