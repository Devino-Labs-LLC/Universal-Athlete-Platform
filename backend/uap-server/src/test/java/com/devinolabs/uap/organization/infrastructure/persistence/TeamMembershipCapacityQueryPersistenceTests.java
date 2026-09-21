package com.devinolabs.uap.organization.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.CreateTeamUseCase;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;

@SpringBootTest(properties = {
		"uap.billing.organization-capacity-enforcement.enabled=true",
		"uap.billing.entitlement-enforcement.enabled=false",
		"uap.billing.stripe.enabled=false"
})
@Import(TestcontainersConfiguration.class)
class TeamMembershipCapacityQueryPersistenceTests {

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private CreateTeamUseCase createTeamUseCase;

	@Autowired
	private TeamMembershipRepository teamMembershipRepository;

	@Autowired
	private Clock clock;

	@Test
	void distinctActiveAthleteCountIgnoresCoachesLeftRemovedAndCountsSameAthleteOnce() {
		AccountId owner = AccountId.of(UUID.randomUUID());
		OrganizationId organizationId = createOrganizationUseCase.execute(owner, "Count Org").id();
		TeamId teamA = createTeamUseCase.execute(owner, organizationId, "Team A").id();
		TeamId teamB = createTeamUseCase.execute(owner, organizationId, "Team B").id();
		AccountId otherOwner = AccountId.of(UUID.randomUUID());
		OrganizationId otherOrg = createOrganizationUseCase.execute(otherOwner, "Other").id();
		TeamId otherTeam = createTeamUseCase.execute(otherOwner, otherOrg, "Other Team").id();

		UUID sharedAthlete = UUID.randomUUID();
		saveAthlete(teamA, sharedAthlete);
		saveAthlete(teamB, sharedAthlete);
		saveAthlete(teamA, UUID.randomUUID());
		TeamMembership left = saveAthlete(teamA, UUID.randomUUID());
		left.leave(clock);
		teamMembershipRepository.save(left);
		TeamMembership removed = saveAthlete(teamB, UUID.randomUUID());
		removed.remove(clock);
		teamMembershipRepository.save(removed);
		teamMembershipRepository.save(TeamMembership.register(
				TeamMembershipId.generate(),
				teamA,
				AccountId.of(UUID.randomUUID()),
				null,
				OrganizationMembershipRole.COACH,
				clock));
		saveAthlete(otherTeam, UUID.randomUUID());

		assertThat(teamMembershipRepository.countDistinctActiveAthletes(organizationId)).isEqualTo(2);
		assertThat(teamMembershipRepository.existsActiveAthleteInOrganization(organizationId, sharedAthlete)).isTrue();
		assertThat(teamMembershipRepository.existsActiveAthleteInOrganization(organizationId, UUID.randomUUID())).isFalse();
	}

	private TeamMembership saveAthlete(TeamId teamId, UUID athleteId) {
		return teamMembershipRepository.save(TeamMembership.register(
				TeamMembershipId.generate(),
				teamId,
				AccountId.of(UUID.randomUUID()),
				athleteId,
				OrganizationMembershipRole.ATHLETE,
				clock));
	}
}
