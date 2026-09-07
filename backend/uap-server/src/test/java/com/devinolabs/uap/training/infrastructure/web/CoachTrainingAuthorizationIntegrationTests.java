package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;

import javax.sql.DataSource;

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
class CoachTrainingAuthorizationIntegrationTests {

	private static final LocalDate ASSIGNED_DATE = LocalDate.now(java.time.ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

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
	void coachAndHeadCoachAssignWhenCollaborationConsentExists() throws Exception {
		Fixture fx = seed("e-ok", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");

		MvcResult created = assign(fx.coach, fx, "tempo-1", "Tempo intervals", "6 x 400m");
		assertThat(created.getResponse().getStatus()).isEqualTo(201);
		String body = created.getResponse().getContentAsString();
		assertThat(body).contains("COACH_ASSIGNMENT");
		assertThat(body).doesNotContain("readinessScore");
		assertThat(body).doesNotContain("email");
		assertThat(body).doesNotContain("@");

		mockMvc.perform(get(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Tempo intervals"))
				.andExpect(jsonPath("$[0].status").value("ASSIGNED"))
				.andExpect(jsonPath("$[0].provenance").value("COACH_ASSIGNMENT"));

		Fixture head = seed("e-head", "HEAD_COACH");
		grant(head.athlete, head.teamId, "TRAINING_COLLABORATION");
		assign(head.coach, head, "head-1", "Team tempo", null);
	}

	@Test
	void assignmentDoesNotMutateStateReadinessOrRecommendation() throws Exception {
		Fixture fx = seed("e-state", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		String recommendationId = seedRecommendation(fx.athlete, ASSIGNED_DATE);
		long stateBefore = count("SELECT COUNT(*) FROM daily_athlete_state_snapshots");
		long readinessBefore = count("SELECT COUNT(*) FROM daily_readiness_assessments");
		long recommendationBefore = count("SELECT COUNT(*) FROM daily_training_recommendations");
		String recommendationBody = recommendationSnapshot(fx.athlete, recommendationId);

		assign(fx.coach, fx, "state-1", "Tempo intervals", "Coach prescribed session");

		assertThat(count("SELECT COUNT(*) FROM daily_athlete_state_snapshots")).isEqualTo(stateBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_readiness_assessments")).isEqualTo(readinessBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_training_recommendations")).isEqualTo(recommendationBefore);
		assertThat(recommendationSnapshot(fx.athlete, recommendationId)).isEqualTo(recommendationBody);
		assertThat(count("SELECT COUNT(*) FROM training_assignments")).isGreaterThan(0);

		mockMvc.perform(get("/api/v1/athletes/me/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Tempo intervals"))
				.andExpect(jsonPath("$[0].provenance").value("COACH_ASSIGNMENT"))
				.andExpect(jsonPath("$[0].status").value("ASSIGNED"));
	}

	@Test
	void unrelatedScopesAndMissingConsentCannotAssign() throws Exception {
		Fixture fx = seed("e-scope", "COACH");
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("none", "Tempo", null)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));

		grant(fx.athlete, fx.teamId, "READINESS_SCORE");
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("ready", "Tempo", null)))
				.andExpect(status().isNotFound());

		Fixture exportOnly = seed("e-export", "COACH");
		grant(exportOnly.athlete, exportOnly.teamId, "EXPORT");
		mockMvc.perform(post(coachPath(exportOnly))
						.with(ConsentHttpFixtures.accountAuth(exportOnly.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("export", "Tempo", null)))
				.andExpect(status().isNotFound());
	}

	@Test
	void adminRolesAndForeignActorsCannotAssign() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("e-admin-owner");
		VerifiedAccount teamAdmin = accounts.registerVerified("e-admin-ta");
		VerifiedAccount orgAdmin = accounts.registerVerified("e-admin-oa");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("e-admin-athlete");
		VerifiedAccount outsider = accounts.registerVerified("e-admin-out");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "e-admin Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "e-admin Team");
		inviteAccept(owner, teamId, teamAdmin, "TEAM_ADMIN");
		inviteOrgRole(owner, orgId, orgAdmin, "ORG_ADMIN");
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		grant(athlete, teamId, "TRAINING_COLLABORATION");

		for (VerifiedAccount actor : new VerifiedAccount[] { owner, teamAdmin, orgAdmin, outsider, athlete }) {
			mockMvc.perform(post("/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/training/assignments")
							.with(ConsentHttpFixtures.accountAuth(actor.accountId()))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content(assignmentBody("adm-" + actor.email(), "Tempo", null)))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));
		}

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/training/assignments")
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("anon", "Tempo", null)))
				.andExpect(status().isForbidden());
	}

	@Test
	void revokeAndMembershipLossAndRejoinBlockFurtherWrites() throws Exception {
		Fixture fx = seed("e-rev", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		MvcResult created = assign(fx.coach, fx, "rev-1", "Tempo intervals", null);
		String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		revokeAll(fx.athlete);
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("rev-2", "Another", null)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/athletes/me/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(assignmentId));

		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("after-leave", "After leave", null)))
				.andExpect(status().isNotFound());

		String rejoinToken = ConsentHttpFixtures.inviteAthlete(mockMvc, fx.owner.accountId(), fx.teamId, fx.athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, fx.athlete.accountId(), rejoinToken);
		mockMvc.perform(post(coachPath(fx) )
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("m2-old", "Should fail", null)))
				.andExpect(status().isNotFound());

		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		assign(fx.coach, fx, "m2-new", "New generation", null);
	}

	@Test
	void athleteDeclineAndUnableAreNotExecutionAndAreIdempotent() throws Exception {
		Fixture fx = seed("e-resp", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		long occurrencesBefore = count("SELECT COUNT(*) FROM workout_occurrences");
		MvcResult created = assign(fx.coach, fx, "dec-1", "Tempo intervals", null);
		String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "note": "Not today" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DECLINED"))
				.andExpect(jsonPath("$.athleteResponseNote").value("Not today"));
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "note": "Not today" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DECLINED"));
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/unable")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "note": "Injured" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_CONFLICT"));
		assertThat(count("SELECT COUNT(*) FROM workout_occurrences")).isEqualTo(occurrencesBefore);

		Fixture unable = seed("e-un", "COACH");
		grant(unable.athlete, unable.teamId, "TRAINING_COLLABORATION");
		MvcResult unableCreated = assign(unable.coach, unable, "un-1", "Long run", null);
		String unableId = JsonPath.read(unableCreated.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + unableId + "/unable")
						.with(ConsentHttpFixtures.accountAuth(unable.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "note": "Travel" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UNABLE"));
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + unableId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(unable.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(unable.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void duplicateSubmitAndStaleVersionAreIntentional() throws Exception {
		Fixture fx = seed("e-con", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		assign(fx.coach, fx, "idem-1", "Tempo intervals", "first");
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("idem-1", "Tempo intervals", "first")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Tempo intervals"));
		assertThat(count("SELECT COUNT(*) FROM training_assignments WHERE idempotency_key = 'idem-1'")).isEqualTo(1);
		mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("idem-1", "Different", "changed")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_CONFLICT"));

		MvcResult listed = mockMvc.perform(get(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andReturn();
		String assignmentId = JsonPath.read(listed.getResponse().getContentAsString(), "$[0].id");
		mockMvc.perform(patch(coachPath(fx) + "/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "expectedVersion": 99,
								  "title": "Stale",
								  "scheduledDate": "%s"
								}
								""".formatted(ASSIGNED_DATE)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_VERSION_CONFLICT"));
	}

	@Test
	void swappedIdsDoNotLeakAssignmentContent() throws Exception {
		Fixture fx = seed("e-idor", "COACH");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		MvcResult created = assign(fx.coach, fx, "idor-1", "Secret tempo", null);
		String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
		Fixture other = seed("e-idor2", "COACH");
		grant(other.athlete, other.teamId, "TRAINING_COLLABORATION");

		mockMvc.perform(get("/api/v1/teams/" + other.teamId + "/athletes/" + fx.athleteId + "/training/assignments/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(other.coach.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));
		String body = mockMvc.perform(get("/api/v1/teams/" + other.teamId + "/athletes/" + other.athleteId + "/training/assignments/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(other.coach.accountId())))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).doesNotContain("Secret tempo");
		assertThat(body).doesNotContain(fx.athlete.email());
	}

	private MvcResult assign(VerifiedAccount coach, Fixture fx, String key, String title, String description)
			throws Exception {
		return mockMvc.perform(post(coachPath(fx))
						.with(ConsentHttpFixtures.accountAuth(coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody(key, title, description)))
				.andExpect(status().isCreated())
				.andReturn();
	}

	private Fixture seed(String prefix, String coachRole) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		VerifiedAccount coach = accounts.registerVerified(prefix + "-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete(prefix + "-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, prefix + " Team");
		inviteAccept(owner, teamId, coach, coachRole);
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		return new Fixture(owner, coach, athlete, orgId, teamId, athleteId);
	}

	private void inviteAccept(VerifiedAccount owner, String teamId, VerifiedAccount member, String role)
			throws Exception {
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
		ConsentHttpFixtures.acceptInvite(mockMvc, member.accountId(), token);
	}

	private void inviteOrgRole(VerifiedAccount owner, String orgId, VerifiedAccount member, String role)
			throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "%s" }
								""".formatted(member.email(), role)))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, member.accountId(), token);
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

	private void revokeAll(VerifiedAccount athlete) throws Exception {
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

	private String seedRecommendation(VerifiedAccount athlete, LocalDate date) throws Exception {
		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "%s",
								  "sleepDurationMinutes": 360,
								  "sleepQuality": 2,
								  "fatigue": 5,
								  "muscleSoreness": 4,
								  "stress": 4,
								  "mood": 2,
								  "motivation": 2,
								  "notes": "private recovery note"
								}
								""".formatted(date)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + date)
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "baselineWindowDays": 7 }
								"""))
				.andExpect(status().isOk());
		MvcResult readiness = mockMvc.perform(post("/api/v1/training/readiness/daily/" + date)
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();
		String assessmentId = JsonPath.read(readiness.getResponse().getContentAsString(), "$.assessmentId");
		MvcResult recommendation = mockMvc.perform(post("/api/v1/training/recommendations")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "dailyReadinessAssessmentId": "%s" }
								""".formatted(assessmentId)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(recommendation.getResponse().getContentAsString(), "$.recommendationId");
	}

	private String recommendationSnapshot(VerifiedAccount athlete, String recommendationId) throws Exception {
		return mockMvc.perform(get("/api/v1/training/recommendations/" + recommendationId)
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
	}

	private long count(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static String coachPath(Fixture fx) {
		return "/api/v1/teams/" + fx.teamId + "/athletes/" + fx.athleteId + "/training/assignments";
	}

	private static String assignmentBody(String key, String title, String description) {
		String descriptionJson = description == null ? "null" : "\"" + description + "\"";
		return """
				{
				  "title": "%s",
				  "description": %s,
				  "scheduledDate": "%s",
				  "idempotencyKey": "%s"
				}
				""".formatted(title, descriptionJson, ASSIGNED_DATE, key);
	}

	private record Fixture(
			VerifiedAccount owner,
			VerifiedAccount coach,
			VerifiedAccount athlete,
			String orgId,
			String teamId,
			String athleteId) {
	}

}
