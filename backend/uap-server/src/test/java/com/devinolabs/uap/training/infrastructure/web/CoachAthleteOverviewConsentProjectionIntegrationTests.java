package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
class CoachAthleteOverviewConsentProjectionIntegrationTests {

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
	void noConsentReturnsRosterSafeBaseAndAllSensitiveNotShared() throws Exception {
		Fixture fx = seedTeamWithCoachAndAthlete("ov-none");
		MvcResult result = mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamId").value(fx.teamId()))
				.andExpect(jsonPath("$.athleteId").value(fx.athleteId()))
				.andExpect(jsonPath("$.displayName").value("Test Athlete"))
				.andExpect(jsonPath("$.role").value("ATHLETE"))
				.andExpect(jsonPath("$.effectiveScopes", hasSize(0)))
				.andExpect(jsonPath("$.availability.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.readinessScore.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.limitingDimensions.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.trainingAdherence.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.performanceHistory.status").value("NOT_SHARED"))
				.andReturn();
		assertNoSensitiveLeaks(result.getResponse().getContentAsString());
	}

	@Test
	void categoryOnlyScoreOnlyLimitingOnlyRecoveryOnlyAndMultiScopeUnionAreIndependent() throws Exception {
		Fixture fx = seedTeamWithCoachAndAthlete("ov-scope");

		grantScopes(fx.athlete, fx.teamId(), "READINESS_CATEGORY");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.effectiveScopes", containsInAnyOrder("READINESS_CATEGORY")))
				.andExpect(jsonPath("$.readinessCategory.status").value("NO_DATA"))
				.andExpect(jsonPath("$.readinessScore.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.limitingDimensions.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("NOT_SHARED"));

		revokeAll(fx.athlete);
		grantScopes(fx.athlete, fx.teamId(), "READINESS_SCORE");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readinessScore.status").value("NO_DATA"))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.limitingDimensions.status").value("NOT_SHARED"));

		revokeAll(fx.athlete);
		grantScopes(fx.athlete, fx.teamId(), "LIMITING_DIMENSIONS");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.limitingDimensions.status").value("NO_DATA"))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.readinessScore.status").value("NOT_SHARED"));

		revokeAll(fx.athlete);
		grantScopes(fx.athlete, fx.teamId(), "RECOVERY_CHECK_IN_DETAIL");
		LocalDate viewDate = LocalDate.now();
		createRecoveryCheckIn(fx.athlete, viewDate);
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()) + "?date=" + viewDate)
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("AVAILABLE"))
				.andExpect(jsonPath("$.recoveryCheckIn.data.notes").value("Coach overview note"))
				.andExpect(jsonPath("$.recoveryCheckIn.data.fatigue.value").value(3))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.readinessScore.status").value("NOT_SHARED"));

		revokeAll(fx.athlete);
		grantScopes(fx.athlete, fx.teamId(), "READINESS_CATEGORY", "READINESS_SCORE", "LIMITING_DIMENSIONS");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.effectiveScopes", hasSize(3)))
				.andExpect(jsonPath("$.readinessCategory.status").value("NO_DATA"))
				.andExpect(jsonPath("$.readinessScore.status").value("NO_DATA"))
				.andExpect(jsonPath("$.limitingDimensions.status").value("NO_DATA"))
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("NOT_SHARED"));
	}

	@Test
	void revokeThenOverviewDropsSectionsAndLeaveArchiveReturn404() throws Exception {
		Fixture fx = seedTeamWithCoachAndAthlete("ov-revoke");
		grantScopes(fx.athlete, fx.teamId(), "READINESS_CATEGORY", "RECOVERY_CHECK_IN_DETAIL");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readinessCategory.status").value("NO_DATA"))
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("NO_DATA"));

		revokeAll(fx.athlete);
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.effectiveScopes", hasSize(0)))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"))
				.andExpect(jsonPath("$.recoveryCheckIn.status").value("NOT_SHARED"));

		Fixture leaveAthlete = seedTeamWithCoachAndAthlete("ov-leave-ath");
		mockMvc.perform(post("/api/v1/teams/" + leaveAthlete.teamId() + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(leaveAthlete.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(overviewPath(leaveAthlete.teamId(), leaveAthlete.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(leaveAthlete.coach.accountId())))
				.andExpect(status().isNotFound());

		Fixture leaveCoach = seedTeamWithCoachAndAthlete("ov-leave-coach");
		mockMvc.perform(post("/api/v1/teams/" + leaveCoach.teamId() + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(leaveCoach.coach.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(overviewPath(leaveCoach.teamId(), leaveCoach.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(leaveCoach.coach.accountId())))
				.andExpect(status().isNotFound());

		Fixture archiveTeam = seedTeamWithCoachAndAthlete("ov-archive-team");
		mockMvc.perform(post("/api/v1/teams/" + archiveTeam.teamId() + "/archive")
						.with(ConsentHttpFixtures.accountAuth(archiveTeam.owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(overviewPath(archiveTeam.teamId(), archiveTeam.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(archiveTeam.owner.accountId())))
				.andExpect(status().isNotFound());

		Fixture archiveOrg = seedTeamWithCoachAndAthlete("ov-archive-org");
		mockMvc.perform(post("/api/v1/organizations/" + archiveOrg.orgId() + "/archive")
						.with(ConsentHttpFixtures.accountAuth(archiveOrg.owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(get(overviewPath(archiveOrg.teamId(), archiveOrg.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(archiveOrg.owner.accountId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejoinDoesNotResurrectPriorMembershipConsentAndNoHiddenWrites() throws Exception {
		Fixture fx = seedTeamWithCoachAndAthlete("ov-rejoin");
		grantScopes(fx.athlete, fx.teamId(), "READINESS_CATEGORY");
		String m1 = fx.membershipId();

		mockMvc.perform(post("/api/v1/teams/" + fx.teamId() + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, fx.owner.accountId(), fx.teamId(), fx.athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, fx.athlete.accountId(), token);
		String m2 = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.id");
		assertThat(m2).isNotEqualTo(m1);

		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.membershipId").value(m2))
				.andExpect(jsonPath("$.effectiveScopes", hasSize(0)))
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"));

		long consentBefore = count("SELECT COUNT(*) FROM consent_grants");
		long readinessBefore = count("SELECT COUNT(*) FROM daily_readiness_assessments");
		long membershipBefore = count("SELECT COUNT(*) FROM team_memberships");
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get(overviewPath(fx.teamId(), fx.athleteId()))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId() + "/roster")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());
		assertThat(count("SELECT COUNT(*) FROM consent_grants")).isEqualTo(consentBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_readiness_assessments")).isEqualTo(readinessBefore);
		assertThat(count("SELECT COUNT(*) FROM team_memberships")).isEqualTo(membershipBefore);
	}

	private Fixture seedTeamWithCoachAndAthlete(String prefix) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		VerifiedAccount coach = accounts.registerVerified(prefix + "-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete(prefix + "-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, prefix + " Team");

		MvcResult coachInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(coach.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String coachToken = JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, coach.accountId(), coachToken);

		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		String membershipId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.id");
		return new Fixture(owner, coach, athlete, orgId, teamId, athleteId, membershipId);
	}

	private void grantScopes(VerifiedAccount athlete, String teamId, String... scopes) throws Exception {
		String scopeJson = "[\"" + String.join("\",\"", scopes) + "\"]";
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": %s }
								""".formatted(teamId, scopeJson)))
				.andExpect(status().isCreated());
	}

	private void revokeAll(VerifiedAccount athlete) throws Exception {
		MvcResult list = mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andReturn();
		net.minidev.json.JSONArray consents = JsonPath.read(list.getResponse().getContentAsString(), "$");
		for (Object item : consents) {
			String id = JsonPath.read(item, "$.id");
			String statusValue = JsonPath.read(item, "$.status");
			if ("ACTIVE".equals(statusValue)) {
				mockMvc.perform(post("/api/v1/athletes/me/consents/" + id + "/revoke")
								.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
								.with(csrf()))
						.andExpect(status().isNoContent());
			}
		}
	}

	private void createRecoveryCheckIn(VerifiedAccount athlete, LocalDate date) throws Exception {
		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "%s",
								  "sleepDurationMinutes": 420,
								  "sleepQuality": 3,
								  "fatigue": 3,
								  "muscleSoreness": 2,
								  "stress": 2,
								  "mood": 4,
								  "motivation": 3,
								  "notes": "Coach overview note"
								}
								""".formatted(date)))
				.andExpect(status().isCreated());
	}

	private long count(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static String overviewPath(String teamId, String athleteId) {
		return "/api/v1/teams/" + teamId + "/athletes/" + athleteId + "/overview";
	}

	private static void assertNoSensitiveLeaks(String body) {
		assertThat(body).doesNotContain("limitingDimensions.data");
		assertThat(body).doesNotContain("email");
		assertThat(body).doesNotContain("@example.com");
		assertThat(body).doesNotContain("accountId");
		assertThat(body).doesNotContain("contributions");
		assertThat(body).doesNotContain("strongestDimensions");
	}

	private record Fixture(
			VerifiedAccount owner,
			VerifiedAccount coach,
			VerifiedAccount athlete,
			String orgId,
			String teamId,
			String athleteId,
			String membershipId) {
	}

}
