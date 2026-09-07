package com.devinolabs.uap.organization.application;

import java.time.Clock;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.api.AccountDirectoryPort;
import com.devinolabs.uap.identity.api.AccountDirectoryRef;
import com.devinolabs.uap.identity.api.SecureTokenDigestPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.Invitation;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.InvitationStatus;

@Service
public class DeclineInvitationUseCase {

	private final InvitationRepository invitationRepository;
	private final AccountDirectoryPort accountDirectoryPort;
	private final SecureTokenDigestPort tokenDigestPort;
	private final OrganizationAuditPort auditPort;
	private final Clock clock;

	public DeclineInvitationUseCase(
			InvitationRepository invitationRepository,
			AccountDirectoryPort accountDirectoryPort,
			SecureTokenDigestPort tokenDigestPort,
			OrganizationAuditPort auditPort,
			Clock clock) {
		this.invitationRepository = Objects.requireNonNull(invitationRepository);
		this.accountDirectoryPort = Objects.requireNonNull(accountDirectoryPort);
		this.tokenDigestPort = Objects.requireNonNull(tokenDigestPort);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void executeByRawToken(AccountId accountId, String rawToken) {
		Invitation invitation = loadByRawTokenForUpdate(rawToken);
		decline(accountId, invitation);
	}

	@Transactional
	public void executeByInvitationId(AccountId accountId, InvitationId invitationId) {
		Invitation invitation = invitationRepository.findByIdForUpdate(invitationId)
				.orElseThrow(InvitationNotFoundException::new);
		decline(accountId, invitation);
	}

	private Invitation loadByRawTokenForUpdate(String rawToken) {
		if (rawToken == null || rawToken.isBlank()) {
			throw new InvitationNotFoundException();
		}
		return invitationRepository.findByTokenHashForUpdate(tokenDigestPort.digest(rawToken))
				.orElseThrow(InvitationNotFoundException::new);
	}

	private void decline(AccountId accountId, Invitation invitation) {
		AccountDirectoryRef account = accountDirectoryPort.findByAccountId(accountId.value())
				.orElseThrow(InvitationNotFoundException::new);
		String email = account.normalizedEmail().trim().toLowerCase(Locale.ROOT);
		if (!invitation.invitedEmail().equals(email)) {
			throw new InvitationNotFoundException();
		}
		if (invitation.status() == InvitationStatus.DECLINED) {
			return;
		}
		if (invitation.status() != InvitationStatus.PENDING) {
			throw new InvitationNotFoundException();
		}
		if (invitation.isExpired(clock)) {
			invitation.markExpired(clock);
			invitationRepository.save(invitation);
			throw new InvitationNotFoundException();
		}
		invitation.decline(clock);
		invitationRepository.save(invitation);
		auditPort.invitationDeclined(invitation.id(), invitation.organizationId(), accountId);
	}

}
