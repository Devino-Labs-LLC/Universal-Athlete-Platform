package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneOffset;

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
import com.devinolabs.uap.billing.application.EntitlementEnforcementProperties;
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
class EntitlementEnforcementDisabledHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private EntitlementEnforcementProperties enforcementProperties;

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
	void defaultFlagIsFalseAndGatedFamiliesKeepPreSliceCBehaviorWithoutSubscription() throws Exception {
		assertThat(enforcementProperties.isEnabled()).isFalse();

		VerifiedAccount owner = accounts.registerVerified("off-owner");
		VerifiedAccount coach = accounts.registerVerified("off-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("off-athlete");
		VerifiedAccount admin = accounts.registerVerified("off-admin");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Off Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Off Team");

		mockMvc.perform(patch("/api/v1/teams/" + teamId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Off Team Renamed", "expectedVersion": 0 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Off Team Renamed"));

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(admin.email())))
				.andExpect(status().isCreated());

		MvcResult coachInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(coach.email())))
				.andExpect(status().isCreated())
				.andReturn();
		ConsentHttpFixtures.acceptInvite(
				mockMvc,
				coach.accountId(),
				JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.rawToken"));
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["TRAINING_COLLABORATION"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title": "Off Tempo",
								  "scheduledDate": "%s",
								  "idempotencyKey": "off-1"
								}
								""".formatted(LocalDate.now(ZoneOffset.UTC))))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/overview")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + teamId + "/readiness")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}
}
