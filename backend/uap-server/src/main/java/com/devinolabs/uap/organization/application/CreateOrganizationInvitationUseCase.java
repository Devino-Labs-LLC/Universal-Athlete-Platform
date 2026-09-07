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
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.OrganizationStatus;

@Service
public class CreateOrganizationInvitationUseCase {

	private final OrganizationRepository organizationRepository;
	private final InvitationRepository invitationRepository;
	private final OrganizationMembershipRepository membershipRepository;
	private final OrganizationAccessGuard accessGuard;
	private final AccountDirectoryPort accountDirectoryPort;
	private final SecureTokenDigestPort tokenDigestPort;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public CreateOrganizationInvitationUseCase(
			OrganizationRepository organizationRepository,
			InvitationRepository invitationRepository,
			OrganizationMembershipRepository membershipRepository,
			OrganizationAccessGuard accessGuard,
			AccountDirectoryPort accountDirectoryPort,
			SecureTokenDigestPort tokenDigestPort,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.organizationRepository = Objects.requireNonNull(organizationRepository);
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.membershipRepository = Objects.requireNonNull(membershipRepository);
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.accountDirectoryPort = Objects.requireNonNull(accountDirectoryPort);
		this.tokenDigestPort = Objects.requireNonNull(tokenDigestPort);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public InvitationResult execute(
			AccountId actorAccountId,
			OrganizationId organizationId,
			String email,
			OrganizationMembershipRole role) {
		Objects.requireNonNull(actorAccountId, "actorAccountId must not be null");
		Objects.requireNonNull(organizationId, "organizationId must not be null");
		Objects.requireNonNull(role, "role must not be null");
		if (role != OrganizationMembershipRole.ORG_ADMIN) {
			throw new IllegalArgumentException("Organization invitations may only invite ORG_ADMIN");
		}

		accessGuard.requireOrgAdminOrOwner(actorAccountId, organizationId);
		Organization organization = organizationRepository.findById(organizationId)
				.orElseThrow(OrganizationNotFoundException::new);
		if (organization.status() == OrganizationStatus.ARCHIVED) {
			throw new OrganizationArchivedException();
		}

		String normalizedEmail = requireEmail(email);
		AccountDirectoryRef boundAccount = accountDirectoryPort.findByNormalizedEmail(normalizedEmail).orElse(null);
		if (boundAccount != null
				&& membershipRepository.existsActiveMembership(AccountId.of(boundAccount.accountId()), organizationId)) {
			throw new MembershipConflictException(
					"MEMBERSHIP_ALREADY_ACTIVE",
					"Account already has an active organization membership");
		}

		IssuedInvitation issued = Invitation.issue(
				InvitationId.generate(),
				organizationId,
				null,
				normalizedEmail,
				boundAccount == null ? null : AccountId.of(boundAccount.accountId()),
				OrganizationMembershipRole.ORG_ADMIN,
				actorAccountId,
				tokenDigestPort::digest,
				clock);

		try {
			Invitation saved = invitationRepository.save(issued.invitation());
			auditPort.invitationCreated(saved.id(), saved.organizationId(), null, saved.role(), actorAccountId);
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
