package com.devinolabs.uap.organization.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

/**
 * Slice H stale-authorization battery: the same authenticated session must lose
 * authority after membership, consent, or archive changes (server authZ, not JWT
 * privilege trust).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class V3StaleAuthorizationIntegrationTests {

	private static final LocalDate ASSIGNED_DATE = LocalDate.now(ZoneOffset.UTC);

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
	void afterCoachMembershipRemovedSameSessionLosesRosterReadinessAndAssign() throws Exception {
		Fixture fx = seedWithCollaboration("stale-coach-rm");
		assertCoachCanReadAndAssign(fx, "pre-rm");

		mockMvc.perform(delete("/api/v1/teams/" + fx.teamId + "/memberships/" + fx.coachMembershipId)
						.with(ConsentHttpFixtures.accountAuth(fx.owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertCoachSessionDeniedRosterAndReadiness(fx);
		assertAssignNotFound(fx, "post-rm");
	}

	@Test
	void afterTrainingCollaborationRevokedSameCoachSessionCannotAssign() throws Exception {
		Fixture fx = seedWithCollaboration("stale-consent-rev");
		assertAssignCreated(fx, "pre-rev");

		revokeActiveConsents(fx.athlete);

		assertAssignNotFound(fx, "post-rev");
	}

	@Test
	void afterTeamArchivedSameCoachSessionLosesRosterAndReadiness() throws Exception {
		Fixture fx = seedWithCollaboration("stale-team-arch");
		assertCoachCanReadRosterAndReadiness(fx);

		mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(fx.owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertCoachSessionDeniedRosterAndReadiness(fx);
		assertAssignNotFound(fx, "post-arch");
	}

	@Test
	void afterAthleteLeaveAndRejoinOldConsentDoesNotRestoreCollaborationWrite() throws Exception {
		Fixture fx = seedWithCollaboration("stale-m2-rejoin");
		assertAssignCreated(fx, "m1-ok");

		mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		assertAssignNotFound(fx, "after-leave");

		String rejoinToken = ConsentHttpFixtures.inviteAthlete(
				mockMvc, fx.owner.accountId(), fx.teamId, fx.athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, fx.athlete.accountId(), rejoinToken);
		String rejoinedAthleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		Fixture m2 = new Fixture(
				fx.owner, fx.coach, fx.athlete, fx.orgId, fx.teamId, rejoinedAthleteId, fx.coachMembershipId);

		// M1 TRAINING_COLLABORATION must not authorize writes on M2 without a new grant.
		assertAssignNotFound(m2, "m2-old-consent");

		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		assertAssignCreated(m2, "m2-new-grant");
	}

	private void assertCoachCanReadAndAssign(Fixture fx, String key) throws Exception {
		assertCoachCanReadRosterAndReadiness(fx);
		assertAssignCreated(fx, key);
	}

	private void assertCoachCanReadRosterAndReadiness(Fixture fx) throws Exception {
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/roster")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get(readinessPath(fx.teamId))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());
	}

	private void assertCoachSessionDeniedRosterAndReadiness(Fixture fx) throws Exception {
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/roster")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
		mockMvc.perform(get(readinessPath(fx.teamId))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_READINESS_NOT_FOUND"));
	}

	private void assertAssignCreated(Fixture fx, String key) throws Exception {
		mockMvc.perform(post(assignmentPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody(key, "Stale auth tempo")))
				.andExpect(status().isCreated());
	}

	private void assertAssignNotFound(Fixture fx, String key) throws Exception {
		mockMvc.perform(post(assignmentPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody(key, "Should be denied")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));
	}

	private Fixture seedWithCollaboration(String prefix) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		VerifiedAccount coach = accounts.registerVerified(prefix + "-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete(prefix + "-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, prefix + " Team");
		String coachMembershipId = inviteAcceptMembershipId(owner, teamId, coach, "COACH");
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		grant(athlete, teamId, "TRAINING_COLLABORATION");
		return new Fixture(owner, coach, athlete, orgId, teamId, athleteId, coachMembershipId);
	}

	private String inviteAcceptMembershipId(
			VerifiedAccount owner, String teamId, VerifiedAccount member, String role) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "%s" }
								""".formatted(member.email(), role)))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, member.accountId(), token);
		return JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.id");
	}

	private void grant(VerifiedAccount athlete, String teamId, String scope) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["%s"] }
								""".formatted(teamId, scope)))
				.andExpect(status().isCreated());
	}

	private void revokeActiveConsents(VerifiedAccount athlete) throws Exception {
		MvcResult list = mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andReturn();
		net.minidev.json.JSONArray consents = JsonPath.read(list.getResponse().getContentAsString(), "$");
		for (Object item : consents) {
			if ("ACTIVE".equals(JsonPath.read(item, "$.status"))) {
				String id = JsonPath.read(item, "$.id");
				mockMvc.perform(post("/api/v1/athletes/me/consents/" + id + "/revoke")
								.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
								.with(csrf()))
						.andExpect(status().isNoContent());
			}
		}
	}

	private static String assignmentPath(Fixture fx) {
		return "/api/v1/teams/" + fx.teamId + "/athletes/" + fx.athleteId + "/training/assignments";
	}

	private static String readinessPath(String teamId) {
		return "/api/v1/teams/" + teamId + "/readiness?date=" + ASSIGNED_DATE;
	}

	private static String assignmentBody(String key, String title) {
		return """
				{
				  "title": "%s",
				  "description": null,
				  "scheduledDate": "%s",
				  "idempotencyKey": "%s"
				}
				""".formatted(title, ASSIGNED_DATE, key);
	}

	private record Fixture(
			VerifiedAccount owner,
			VerifiedAccount coach,
			VerifiedAccount athlete,
			String orgId,
			String teamId,
			String athleteId,
			String coachMembershipId) {
	}

}
