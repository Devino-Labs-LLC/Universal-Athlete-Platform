package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MembershipLifecycleIntegrationTests {

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
	void athleteRequiresProfileCoachDoesNotMultiTeamCrossOrgRemoveLeaveAndStaleId() throws Exception {
		VerifiedAccount ownerA = accounts.registerVerified("life-owner-a");
		VerifiedAccount ownerB = accounts.registerVerified("life-owner-b");
		VerifiedAccount athleteNoProfile = accounts.registerVerified("life-athlete-np");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("life-athlete");
		VerifiedAccount coach = accounts.registerVerified("life-coach");

		String orgA = createOrg(ownerA.accountId(), "Life Org A");
		String orgB = createOrg(ownerB.accountId(), "Life Org B");
		String teamA1 = createTeam(ownerA.accountId(), orgA, "Team A1");
		String teamA2 = createTeam(ownerA.accountId(), orgA, "Team A2");
		String teamB = createTeam(ownerB.accountId(), orgB, "Team B");

		String rawNoProfile = invite(ownerA.accountId(), teamA1, athleteNoProfile.email(), "ATHLETE");
		mockMvc.perform(post("/api/v1/invitations/" + rawNoProfile + "/accept")
						.with(accountAuth(athleteNoProfile.accountId()))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_REQUIRED"));

		String athleteToken1 = invite(ownerA.accountId(), teamA1, athlete.email(), "ATHLETE");
		MvcResult accepted1 = mockMvc.perform(post("/api/v1/invitations/" + athleteToken1 + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamMembership.athleteId").isNotEmpty())
				.andReturn();
		String membershipId = JsonPath.read(accepted1.getResponse().getContentAsString(), "$.teamMembership.id");

		String athleteToken2 = invite(ownerA.accountId(), teamA2, athlete.email(), "ATHLETE");
		mockMvc.perform(post("/api/v1/invitations/" + athleteToken2 + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		String athleteTokenB = invite(ownerB.accountId(), teamB, athlete.email(), "ATHLETE");
		mockMvc.perform(post("/api/v1/invitations/" + athleteTokenB + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/organizations").with(accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		String coachToken = invite(ownerA.accountId(), teamA1, coach.email(), "COACH");
		mockMvc.perform(post("/api/v1/invitations/" + coachToken + "/accept")
						.with(accountAuth(coach.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamMembership.athleteId").value(org.hamcrest.Matchers.nullValue()));

		mockMvc.perform(delete("/api/v1/teams/" + teamA1 + "/memberships/" + membershipId)
						.with(accountAuth(ownerA.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/teams/" + teamA1).with(accountAuth(athlete.accountId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/teams/" + teamA1 + "/memberships").with(accountAuth(athlete.accountId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/teams/" + teamA2 + "/memberships/me/leave")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/teams/" + teamA2).with(accountAuth(athlete.accountId())))
				.andExpect(status().isNotFound());
	}

	private String invite(AccountId actor, String teamId, String email, String role) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(actor))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "%s" }
								""".formatted(email, role)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
	}

	private String createOrg(AccountId accountId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createTeam(AccountId accountId, String organizationId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static RequestPostProcessor accountAuth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

}
