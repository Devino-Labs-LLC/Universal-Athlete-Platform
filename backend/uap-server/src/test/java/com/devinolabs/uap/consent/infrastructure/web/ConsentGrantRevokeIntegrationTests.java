package com.devinolabs.uap.consent.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
class ConsentGrantRevokeIntegrationTests {

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
	void grantRevokeIdempotentForeign404AndCoachDenied() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-rev-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-rev-athlete");
		VerifiedAccount otherAthlete = accounts.registerVerifiedAthlete("consent-rev-other");
		VerifiedAccount coach = accounts.registerVerified("consent-rev-coach");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Revoke Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Revoke Team");

		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);

		String coachToken = inviteCoach(owner, teamId, coach.email());
		mockMvc.perform(post("/api/v1/invitations/" + coachToken + "/accept")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		MvcResult granted = mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_CATEGORY", "AVAILABILITY"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.scopes", hasSize(2)))
				.andReturn();
		String consentId = JsonPath.read(granted.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("REVOKED"));

		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(otherAthlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamId)))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isNotFound());
	}

	private String inviteCoach(VerifiedAccount owner, String teamId, String email) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
	}

}
