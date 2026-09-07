package com.devinolabs.uap.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class InvitationTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC);
	private final Function<String, String> digester = token -> "hash-" + token;

	@Test
	void issueCreatesPendingTeamInvitationWithHashedTokenAndSevenDayTtl() {
		IssuedInvitation issued = Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				TeamId.generate(),
				"Athlete@Example.COM",
				null,
				OrganizationMembershipRole.ATHLETE,
				AccountId.generate(),
				digester,
				clock);

		assertThat(issued.rawToken()).isNotBlank();
		assertThat(issued.invitation().tokenHash()).isEqualTo("hash-" + issued.rawToken());
		assertThat(issued.invitation().invitedEmail()).isEqualTo("athlete@example.com");
		assertThat(issued.invitation().status()).isEqualTo(InvitationStatus.PENDING);
		assertThat(issued.invitation().expiresAt()).isEqualTo(Instant.parse("2026-09-13T12:00:00Z"));
		assertThat(issued.invitation().isEffective(clock)).isTrue();
	}

	@Test
	void acceptDeclineAndRevokeTransitionPendingOnly() {
		IssuedInvitation issued = Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				null,
				"admin@example.com",
				null,
				OrganizationMembershipRole.ORG_ADMIN,
				AccountId.generate(),
				digester,
				clock);
		Invitation invitation = issued.invitation();
		UUID membershipId = UUID.randomUUID();

		invitation.acceptPending(membershipId, clock);
		assertThat(invitation.status()).isEqualTo(InvitationStatus.ACCEPTED);
		assertThat(invitation.acceptedMembershipId()).isEqualTo(membershipId);

		IssuedInvitation pending = Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				TeamId.generate(),
				"coach@example.com",
				null,
				OrganizationMembershipRole.COACH,
				AccountId.generate(),
				digester,
				clock);
		pending.invitation().decline(clock);
		assertThat(pending.invitation().status()).isEqualTo(InvitationStatus.DECLINED);

		IssuedInvitation revokable = Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				TeamId.generate(),
				"head@example.com",
				null,
				OrganizationMembershipRole.HEAD_COACH,
				AccountId.generate(),
				digester,
				clock);
		revokable.invitation().revoke(clock);
		assertThat(revokable.invitation().status()).isEqualTo(InvitationStatus.REVOKED);
	}

	@Test
	void expiredInvitationIsNotEffectiveAndCannotAccept() {
		IssuedInvitation issued = Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				TeamId.generate(),
				"athlete@example.com",
				null,
				OrganizationMembershipRole.ATHLETE,
				AccountId.generate(),
				digester,
				clock);
		Clock afterExpiry = Clock.fixed(Instant.parse("2026-09-13T12:00:00Z"), ZoneOffset.UTC);
		assertThat(issued.invitation().isEffective(afterExpiry)).isFalse();
		assertThatThrownBy(() -> issued.invitation().acceptPending(UUID.randomUUID(), afterExpiry))
				.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void orgScopedInvitationRequiresOrgAdminRole() {
		assertThatThrownBy(() -> Invitation.issue(
				InvitationId.generate(),
				OrganizationId.generate(),
				null,
				"owner@example.com",
				null,
				OrganizationMembershipRole.ORG_OWNER,
				AccountId.generate(),
				digester,
				clock)).isInstanceOf(IllegalArgumentException.class);
	}

}
