package com.devinolabs.uap.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TeamMembershipTest {

	private final Clock clock = Clock.fixed(Instant.parse("2026-09-06T12:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerAthleteRequiresAthleteIdAndRemoveLeaveTransitions() {
		UUID athleteId = UUID.randomUUID();
		TeamMembership membership = TeamMembership.register(
				TeamMembershipId.generate(),
				TeamId.generate(),
				AccountId.generate(),
				athleteId,
				OrganizationMembershipRole.ATHLETE,
				clock);

		assertThat(membership.status()).isEqualTo(OrganizationMembershipStatus.ACTIVE);
		assertThat(membership.athleteId()).isEqualTo(athleteId);

		membership.remove(clock);
		assertThat(membership.status()).isEqualTo(OrganizationMembershipStatus.REMOVED);
	}

	@Test
	void coachMembershipHasNullAthleteIdAndCanLeave() {
		TeamMembership membership = TeamMembership.register(
				TeamMembershipId.generate(),
				TeamId.generate(),
				AccountId.generate(),
				null,
				OrganizationMembershipRole.COACH,
				clock);
		membership.leave(clock);
		assertThat(membership.status()).isEqualTo(OrganizationMembershipStatus.LEFT);
	}

	@Test
	void rejectsOrgRolesAndAthleteWithoutAthleteId() {
		assertThatThrownBy(() -> TeamMembership.register(
				TeamMembershipId.generate(),
				TeamId.generate(),
				AccountId.generate(),
				null,
				OrganizationMembershipRole.ORG_ADMIN,
				clock)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> TeamMembership.register(
				TeamMembershipId.generate(),
				TeamId.generate(),
				AccountId.generate(),
				null,
				OrganizationMembershipRole.ATHLETE,
				clock)).isInstanceOf(IllegalArgumentException.class);
	}

}
