package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CoachRosterAuthorizationIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	private VerifiedAccountFixture accounts;

	@BeforeEach
	void setUp() {
		accounts = new VerifiedAccountFixture(
				registerAccountUseCase,
				verifyEmailUseCase,
				verificationNotifier,
				createAthleteProfileUseCase);
	}

	@Test
	void authorizedCoachSeesAthletesForeignRemovedAndLeftExcludedCoachesExcludedNoSensitiveFields() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("roster-owner");
		VerifiedAccount coach = accounts.registerVerified("roster-coach");
		VerifiedAccount foreign = accounts.registerVerified("roster-foreign");
		VerifiedAccount athleteA = accounts.registerVerifiedAthlete("roster-athlete-a");
		VerifiedAccount athleteB = accounts.registerVerifiedAthlete("roster-athlete-b");
		VerifiedAccount athleteLeft = accounts.registerVerifiedAthlete("roster-athlete-left");
		VerifiedAccount athleteRemoved = accounts.registerVerifiedAthlete("roster-athlete-removed");
		VerifiedAccount multiTeamAthlete = accounts.registerVerifiedAthlete("roster-multi");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Roster Org");
		String team1 = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Roster Team 1");
		String team2 = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Roster Team 2");

		acceptCoach(owner, team1, coach);
		String athleteAId = acceptAthlete(owner, team1, athleteA);
		String athleteBId = acceptAthlete(owner, team1, athleteB);
		String leftMembershipId = acceptAthleteMembershipId(owner, team1, athleteLeft);
		String removedMembershipId = acceptAthleteMembershipId(owner, team1, athleteRemoved);
		String multiTeam1AthleteId = acceptAthlete(owner, team1, multiTeamAthlete);
		String multiTeam2AthleteId = acceptAthlete(owner, team2, multiTeamAthlete);
		assertThat(multiTeam1AthleteId).isEqualTo(multiTeam2AthleteId);

		mockMvc.perform(post("/api/v1/teams/" + team1 + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(athleteLeft.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(delete("/api/v1/teams/" + team1 + "/memberships/" + removedMembershipId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		MvcResult roster = mockMvc.perform(get("/api/v1/teams/" + team1 + "/roster")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(3)))
				.andReturn();
		String body = roster.getResponse().getContentAsString();
		assertThat(body).doesNotContain("email");
		assertThat(body).doesNotContain("@example.com");
		assertThat(body).doesNotContain("readiness");
		assertThat(body).doesNotContain("recovery");
		assertThat(body).doesNotContain("accountId");
		assertThat(body).doesNotContain("phone");
		assertThat(body).doesNotContain(athleteLeft.email());
		assertThat(body).doesNotContain(leftMembershipId);
		assertThat(body).contains(athleteAId);
		assertThat(body).contains(athleteBId);
		assertThat(body).contains(multiTeam1AthleteId);
		assertThat(body).doesNotContain("\"role\":\"COACH\"");

		mockMvc.perform(get("/api/v1/teams/" + team2 + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].athleteId").value(multiTeam2AthleteId));

		mockMvc.perform(get("/api/v1/teams/" + team1 + "/roster")
						.with(ConsentHttpFixtures.accountAuth(foreign.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/teams/" + team1 + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/teams/" + team1 + "/roster")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
	}

	@Test
	void repeatedRosterGetDoesNotCreateState() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("roster-nhw-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("roster-nhw-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Roster NHW Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Roster NHW Team");
		acceptAthlete(owner, teamId, athlete);

		String first = mockMvc.perform(get("/api/v1/teams/" + teamId + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String second = mockMvc.perform(get("/api/v1/teams/" + teamId + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(second).isEqualTo(first);
	}

	@Test
	void archivedTeamAndOrganizationRosterReturnNotFound() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("roster-arch-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("roster-arch-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Roster Arch Org");
		String teamArchived = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Roster Arch Team");
		String orgArchivedId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Roster Org Arch");
		String teamUnderArchivedOrg =
				ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgArchivedId, "Roster Org Arch Team");
		acceptAthlete(owner, teamArchived, athlete);
		acceptAthlete(owner, teamUnderArchivedOrg, athlete);

		mockMvc.perform(post("/api/v1/teams/" + teamArchived + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/teams/" + teamArchived + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/organizations/" + orgArchivedId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/teams/" + teamUnderArchivedOrg + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
	}

	private void acceptCoach(VerifiedAccount owner, String teamId, VerifiedAccount coach) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(coach.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, coach.accountId(), token);
	}

	private String acceptAthlete(VerifiedAccount owner, String teamId, VerifiedAccount athlete) throws Exception {
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
		return JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
	}

	private String acceptAthleteMembershipId(VerifiedAccount owner, String teamId, VerifiedAccount athlete)
			throws Exception {
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
		return JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.id");
	}

}
