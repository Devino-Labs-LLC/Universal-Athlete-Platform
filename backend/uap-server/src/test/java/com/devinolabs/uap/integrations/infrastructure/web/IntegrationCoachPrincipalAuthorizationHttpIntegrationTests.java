package com.devinolabs.uap.integrations.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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

/**
 * Coach-of-athlete must not expand into Connected Athlete connection/evidence access.
 * Ownership remains the athlete account that created the connection (ActiveConnectionPolicy).
 */
@SpringBootTest(properties = {
		"uap.integrations.enabled=true",
		"uap.integrations.apple-healthkit.enabled=true",
		"uap.integrations.health-connect.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IntegrationCoachPrincipalAuthorizationHttpIntegrationTests {

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
	void coachOfAthleteCannotAccessOrMutateAthleteConnectionOrEvidence() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("int-coach-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("int-coach-athlete");
		// Coach also has an athlete profile so requireAthlete succeeds; coaching must still not grant access.
		VerifiedAccount coach = accounts.registerVerifiedAthlete("int-coach-principal");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Integrations Coach Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Integrations Coach Team");
		acceptCoach(owner, teamId, coach);
		acceptAthlete(owner, teamId, athlete);

		UUID connectionId = UUID.randomUUID();
		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(connectionId)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/confirm")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId)
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId + "/evidence")
						.param("from", "2026-09-01T00:00:00Z")
						.param("to", "2026-09-30T23:59:59Z")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "requestId":"%s",
								  "items":[{
								    "externalRecordId":"coach-forbid-1",
								    "signalFamily":"SLEEP",
								    "signalType":"DURATION",
								    "valueNumeric":60,
								    "unitCode":"MINUTE",
								    "observedAt":"2026-09-29T06:00:00Z",
								    "provenanceClass":"CLIENT_DEVICE"
								  }]
								}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/sync")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/disconnect")
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId)
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CONNECTED"));
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

	private void acceptAthlete(VerifiedAccount owner, String teamId, VerifiedAccount athlete) throws Exception {
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
	}
}
