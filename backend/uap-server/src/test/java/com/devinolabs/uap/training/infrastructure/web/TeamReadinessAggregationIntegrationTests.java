package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
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
class TeamReadinessAggregationIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void unauthenticatedAndAthleteCannotReadTeamReadiness() throws Exception {
		Fixture fx = seedTeam("tr-auth");
		mockMvc.perform(get(path(fx.teamId, LocalDate.now(ZoneOffset.UTC))))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get(path(fx.teamId, LocalDate.now(ZoneOffset.UTC)))
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_READINESS_NOT_FOUND"));
	}

	@Test
	void coachHeadCoachTeamAdminAndOrgRolesMayReadWithoutHiddenWrites() throws Exception {
		Fixture fx = seedTeam("tr-roles");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		assertReadable(fx.coach, fx.teamId, date);
		assertReadable(inviteRole(fx, "head", "HEAD_COACH"), fx.teamId, date);
		assertReadable(inviteRole(fx, "tadmin", "TEAM_ADMIN"), fx.teamId, date);
		assertReadable(inviteOrgAdmin(fx, "oadmin"), fx.teamId, date);
		assertReadable(fx.owner, fx.teamId, date);

		long stateBefore = count("SELECT COUNT(*) FROM daily_athlete_state_snapshots");
		long readinessBefore = count("SELECT COUNT(*) FROM daily_readiness_assessments");
		long recommendationBefore = count("SELECT COUNT(*) FROM daily_training_recommendations");
		long checkInBefore = count("SELECT COUNT(*) FROM daily_recovery_check_ins");
		long consentBefore = count("SELECT COUNT(*) FROM consent_grants");
		long membershipBefore = count("SELECT COUNT(*) FROM team_memberships");
		long assignmentBefore = count("SELECT COUNT(*) FROM training_assignments");

		mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());

		assertThat(count("SELECT COUNT(*) FROM daily_athlete_state_snapshots")).isEqualTo(stateBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_readiness_assessments")).isEqualTo(readinessBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_training_recommendations")).isEqualTo(recommendationBefore);
		assertThat(count("SELECT COUNT(*) FROM daily_recovery_check_ins")).isEqualTo(checkInBefore);
		assertThat(count("SELECT COUNT(*) FROM consent_grants")).isEqualTo(consentBefore);
		assertThat(count("SELECT COUNT(*) FROM team_memberships")).isEqualTo(membershipBefore);
		assertThat(count("SELECT COUNT(*) FROM training_assignments")).isEqualTo(assignmentBefore);
	}

	@Test
	void foreignAndRemovedViewersAreNotFoundAndResponseLeaksNoIdentity() throws Exception {
		Fixture fx = seedTeam("tr-idor");
		Fixture other = seedTeam("tr-other");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);

		mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(other.coach.accountId())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		String body = mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isNotFound())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).doesNotContain(fx.athleteId);
		assertThat(body).doesNotContain(fx.athlete.email());
		assertThat(body).doesNotContain("athleteId");
		assertThat(body).doesNotContain("membershipId");
	}

	@Test
	void missingConsentAndScoreOnlyConsentDoNotPublishCategoryCounts() throws Exception {
		Fixture fx = seedTeam("tr-consent");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		grantScopes(fx.athlete, fx.teamId, "READINESS_SCORE");

		String body = mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$.cohort").value("BELOW_MINIMUM"))
				.andExpect(jsonPath("$.includedCount").doesNotExist())
				.andExpect(jsonPath("$.availability.status").value("UNSUPPORTED"))
				.andExpect(jsonPath("$.categoryDistribution.cells").isEmpty())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertNoIdentity(body, fx);
	}

	@Test
	void fourConsentedAthletesStayInsufficientAndFiveDoesNotPublishFourPlusOne() throws Exception {
		Fixture fx = seedTeam("tr-cohort");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		List<String> athleteIds = new ArrayList<>();
		athleteIds.add(fx.athleteId);
		grantScopes(fx.athlete, fx.teamId, "READINESS_CATEGORY");
		seedStoredReadiness(fx.athlete, date);
		setBand(fx.athleteId, "HIGH");

		for (int i = 2; i <= 4; i++) {
			VerifiedAccount athlete = accounts.registerVerifiedAthlete("tr-cohort-" + i);
			athleteIds.add(acceptAthlete(fx, athlete));
			grantScopes(athlete, fx.teamId, "READINESS_CATEGORY");
			seedStoredReadiness(athlete, date);
			setBand(athleteIds.get(i - 1), "HIGH");
		}

		mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$.includedCount").doesNotExist());

		VerifiedAccount fifth = accounts.registerVerifiedAthlete("tr-cohort-5");
		String fifthId = acceptAthlete(fx, fifth);
		grantScopes(fifth, fx.teamId, "READINESS_CATEGORY");
		seedStoredReadiness(fifth, date);
		setBand(fifthId, "LOW");

		String body = mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("PUBLISHED"))
				.andExpect(jsonPath("$.cohort").value("AT_LEAST_MINIMUM"))
				.andExpect(jsonPath("$.includedCount").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).doesNotContain("\"count\":4");
		assertThat(body).doesNotContain("\"count\":1");
		assertNoIdentity(body, fx);
	}

	@Test
	void tenAthleteSevenAndThreeDoesNotPublishRecoverableCounts() throws Exception {
		Fixture fx = seedTeam("tr-sub");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		List<SeededAthlete> athletes = new ArrayList<>();
		athletes.add(new SeededAthlete(fx.athlete, fx.athleteId));
		for (int i = 2; i <= 10; i++) {
			VerifiedAccount athlete = accounts.registerVerifiedAthlete("tr-sub-" + i);
			athletes.add(new SeededAthlete(athlete, acceptAthlete(fx, athlete)));
		}
		for (int i = 0; i < athletes.size(); i++) {
			SeededAthlete seeded = athletes.get(i);
			grantScopes(seeded.account, fx.teamId, "READINESS_CATEGORY");
			seedStoredReadiness(seeded.account, date);
			setBand(seeded.athleteId, i < 7 ? "HIGH" : "LOW");
		}

		String body = mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.includedCount").doesNotExist())
				.andReturn()
				.getResponse()
				.getContentAsString();
		assertThat(body).doesNotContain("\"count\":7");
		assertThat(body).doesNotContain("\"count\":3");
		assertThat(body).doesNotContain("\"includedCount\":10");
		assertNoIdentity(body, fx);
	}

	@Test
	void revokedConsentAndRejoinM1DoNotKeepCategoryInclusion() throws Exception {
		Fixture fx = seedTeam("tr-revoke");
		LocalDate date = LocalDate.now(ZoneOffset.UTC);
		grantScopes(fx.athlete, fx.teamId, "READINESS_CATEGORY");
		revokeAll(fx.athlete);

		mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));

		mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, fx.owner.accountId(), fx.teamId, fx.athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, fx.athlete.accountId(), token);

		mockMvc.perform(get(path(fx.teamId, date))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));
	}

	private void assertReadable(VerifiedAccount viewer, String teamId, LocalDate date) throws Exception {
		mockMvc.perform(get(path(teamId, date))
						.with(ConsentHttpFixtures.accountAuth(viewer.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamId").value(teamId))
				.andExpect(jsonPath("$.availability.status").value("UNSUPPORTED"));
	}

	private Fixture seedTeam(String prefix) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		VerifiedAccount coach = accounts.registerVerified(prefix + "-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete(prefix + "-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, prefix + " Team");
		inviteAndAccept(owner, teamId, coach, "COACH");
		String athleteId = acceptAthlete(new Fixture(owner, coach, athlete, orgId, teamId, null), athlete);
		return new Fixture(owner, coach, athlete, orgId, teamId, athleteId);
	}

	private String acceptAthlete(Fixture fx, VerifiedAccount athlete) throws Exception {
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, fx.owner.accountId(), fx.teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
		return JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
	}

	private VerifiedAccount inviteRole(Fixture fx, String prefix, String role) throws Exception {
		VerifiedAccount account = accounts.registerVerified(prefix + "-" + role.toLowerCase());
		inviteAndAccept(fx.owner, fx.teamId, account, role);
		return account;
	}

	private VerifiedAccount inviteOrgAdmin(Fixture fx, String prefix) throws Exception {
		VerifiedAccount account = accounts.registerVerified(prefix + "-org-admin");
		MvcResult invite = mockMvc.perform(post("/api/v1/organizations/" + fx.orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(fx.owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(account.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, account.accountId(), token);
		return account;
	}

	private void inviteAndAccept(VerifiedAccount owner, String teamId, VerifiedAccount account, String role)
			throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "%s" }
								""".formatted(account.email(), role)))
				.andExpect(status().isCreated())
				.andReturn();
		String token = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		ConsentHttpFixtures.acceptInvite(mockMvc, account.accountId(), token);
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
			if ("ACTIVE".equals(JsonPath.read(item, "$.status"))) {
				mockMvc.perform(post("/api/v1/athletes/me/consents/" + id + "/revoke")
								.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
								.with(csrf()))
						.andExpect(status().isNoContent());
			}
		}
	}

	private void seedStoredReadiness(VerifiedAccount athlete, LocalDate date) throws Exception {
		for (int daysBefore = 7; daysBefore >= 1; daysBefore--) {
			createRecoveryCheckIn(athlete, date.minusDays(daysBefore));
		}
		createRecoveryCheckIn(athlete, date);
		MvcResult snapshotResult = mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + date)
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "baselineWindowDays": 7 }
								"""))
				.andExpect(status().isOk())
				.andReturn();
		String snapshotId = JsonPath.read(snapshotResult.getResponse().getContentAsString(), "$.snapshotId");
		mockMvc.perform(post("/api/v1/training/readiness/assessments")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "dailyAthleteStateSnapshotId": "%s" }
								""".formatted(snapshotId)))
				.andExpect(status().isOk());
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
								  "motivation": 3
								}
								""".formatted(date)))
				.andExpect(status().isCreated());
	}

	private void setBand(String athleteId, String band) {
		int updated = jdbcTemplate.update(
				"UPDATE daily_readiness_assessments SET readiness_band = ? WHERE athlete_id = ?",
				band,
				uuidBytes(athleteId));
		assertThat(updated).isGreaterThan(0);
	}

	private static byte[] uuidBytes(String id) {
		UUID uuid = UUID.fromString(id);
		ByteBuffer buffer = ByteBuffer.allocate(16);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());
		return buffer.array();
	}

	private long count(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql);
				ResultSet rs = statement.executeQuery()) {
			rs.next();
			return rs.getLong(1);
		}
	}

	private static void assertNoIdentity(String body, Fixture fx) {
		assertThat(body).doesNotContain(fx.athleteId);
		assertThat(body).doesNotContain(fx.athlete.email());
		assertThat(body).doesNotContain("displayName");
		assertThat(body).doesNotContain("membershipId");
		assertThat(body).doesNotContain("accountId");
	}

	private static String path(String teamId, LocalDate date) {
		return "/api/v1/teams/" + teamId + "/readiness?date=" + date;
	}

	private record Fixture(
			VerifiedAccount owner,
			VerifiedAccount coach,
			VerifiedAccount athlete,
			String orgId,
			String teamId,
			String athleteId) {
	}

	private record SeededAthlete(VerifiedAccount account, String athleteId) {
	}

}
