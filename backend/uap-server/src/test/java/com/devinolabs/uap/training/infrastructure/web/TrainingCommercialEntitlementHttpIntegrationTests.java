package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

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
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.support.OrganizationSubscriptionFixtures;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.billing.entitlement-enforcement.enabled=true",
		"uap.billing.stripe.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingCommercialEntitlementHttpIntegrationTests {

	private static final LocalDate ASSIGNED_DATE = LocalDate.now(ZoneOffset.UTC);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private Clock clock;

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
	void collaborationConsentIsEvaluatedBeforeEntitlement() throws Exception {
		Fixture entitled = seedEntitledCoachTeam("c-collab-ent");
		mockMvc.perform(post(assignmentPath(entitled))
						.with(ConsentHttpFixtures.accountAuth(entitled.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("missing-consent", "Tempo")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));

		Fixture unpaid = seedEntitledCoachTeam("c-collab-unpaid");
		expire(unpaid);
		mockMvc.perform(post(assignmentPath(unpaid))
						.with(ConsentHttpFixtures.accountAuth(unpaid.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("unpaid-missing", "Tempo")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"));
	}

	@Test
	void validCollaborationAndUnpaidOrganizationReturns402WithoutAssignmentWrite() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-assign-402");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		expire(fx);
		Integer before = jdbcTemplate.queryForObject("select count(*) from training_assignments", Integer.class);

		mockMvc.perform(post(assignmentPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("unpaid-write", "Tempo")))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE))
				.andExpect(jsonPath("$.path").value(assignmentPath(fx)));
		assertThat(jdbcTemplate.queryForObject("select count(*) from training_assignments", Integer.class))
				.isEqualTo(before);
	}

	@Test
	void entitledCollaborationAllowsCoachAssignmentFamilyAndAthleteOwnedActionsStayFreeAfterLapse() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-assign-ok");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");

		MvcResult created = mockMvc.perform(post(assignmentPath(fx))
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("ok-1", "Tempo intervals")))
				.andExpect(status().isCreated())
				.andReturn();
		String assignmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(assignmentPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Tempo intervals"));
		mockMvc.perform(get(assignmentPath(fx) + "/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk());

		expire(fx);
		mockMvc.perform(get(assignmentPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
		mockMvc.perform(get(assignmentPath(fx) + "/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
		mockMvc.perform(patch(assignmentPath(fx) + "/" + assignmentId)
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "expectedVersion": 0,
								  "title": "Mutated",
								  "scheduledDate": "%s"
								}
								""".formatted(ASSIGNED_DATE)))
				.andExpect(status().isPaymentRequired());
		mockMvc.perform(get("/api/v1/athletes/me/training/assignments")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Tempo intervals"))
				.andExpect(jsonPath("$[0].version").value(0));
		mockMvc.perform(post("/api/v1/athletes/me/training/assignments/" + assignmentId + "/decline")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "note": "cannot today" }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DECLINED"));
	}

	@Test
	void unauthenticatedGetsNeverReturn402WhetherOrgIsPaidOrNot() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-unauth");
		assertUnauthenticatedGetNever402(overviewPath(fx));
		assertUnauthenticatedGetNever402("/api/v1/teams/" + fx.teamId + "/readiness");
		assertUnauthenticatedGetNever402(assignmentPath(fx));
		expire(fx);
		assertUnauthenticatedGetNever402(overviewPath(fx));
		assertUnauthenticatedGetNever402("/api/v1/teams/" + fx.teamId + "/readiness");
		assertUnauthenticatedGetNever402(assignmentPath(fx));
	}

	@Test
	void insufficientRoleWithActiveOrgNeverReturns402() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-role");
		grant(fx.athlete, fx.teamId, "TRAINING_COLLABORATION");
		VerifiedAccount teamAdmin = accounts.registerVerified("c-role-admin");
		MvcResult adminInvite = mockMvc.perform(post("/api/v1/teams/" + fx.teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(fx.owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "TEAM_ADMIN" }
								""".formatted(teamAdmin.email())))
				.andExpect(status().isCreated())
				.andReturn();
		ConsentHttpFixtures.acceptInvite(
				mockMvc,
				teamAdmin.accountId(),
				JsonPath.read(adminInvite.getResponse().getContentAsString(), "$.rawToken"));

		mockMvc.perform(post(assignmentPath(fx))
						.with(ConsentHttpFixtures.accountAuth(teamAdmin.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assignmentBody("admin-denied", "Tempo")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_ASSIGNMENT_NOT_FOUND"))
				.andExpect(jsonPath("$.code").value(not(CommercialEntitlementRequiredException.CODE)));
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/readiness")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_READINESS_NOT_FOUND"))
				.andExpect(jsonPath("$.code").value(not(CommercialEntitlementRequiredException.CODE)));
	}

	@Test
	void unpaidOverviewWithoutSectionConsentReturns402NotNotShared() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-ov-8e");
		expire(fx);
		mockMvc.perform(get(overviewPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE))
				.andExpect(jsonPath("$.readinessCategory").doesNotExist());
	}

	@Test
	void coachOverviewRequiresEntitlementAfterMembershipAndDoesNotUseSectionConsentAsExistence() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-ov");
		mockMvc.perform(get(overviewPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readinessCategory.status").value("NOT_SHARED"));

		grant(fx.athlete, fx.teamId, "READINESS_CATEGORY");
		mockMvc.perform(get(overviewPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readinessCategory.status").value("NO_DATA"));

		expire(fx);
		mockMvc.perform(get(overviewPath(fx)).with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));

		VerifiedAccount stranger = accounts.registerVerified("c-ov-stranger");
		mockMvc.perform(get(overviewPath(fx)).with(ConsentHttpFixtures.accountAuth(stranger.accountId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void teamReadiness402IsReadOnlyAndInsufficientCohortRemainsExistingBehaviorWhenEntitled() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-ready");
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/readiness")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("INSUFFICIENT_DATA"));

		Integer readinessBefore = jdbcTemplate.queryForObject(
				"select count(*) from daily_readiness_assessments", Integer.class);
		expire(fx);
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/readiness")
						.with(ConsentHttpFixtures.accountAuth(fx.coach.accountId())))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
		assertThat(jdbcTemplate.queryForObject("select count(*) from daily_readiness_assessments", Integer.class))
				.isEqualTo(readinessBefore);

		VerifiedAccount stranger = accounts.registerVerified("c-ready-stranger");
		mockMvc.perform(get("/api/v1/teams/" + fx.teamId + "/readiness")
						.with(ConsentHttpFixtures.accountAuth(stranger.accountId())))
				.andExpect(status().isNotFound());
	}

	@Test
	void athleteOwnedIntelligenceAndConsentRemainFreeAfterBillingLapse() throws Exception {
		Fixture fx = seedEntitledCoachTeam("c-intel");
		expire(fx);
		LocalDate today = LocalDate.now(clock);

		MvcResult granted = mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_CATEGORY"] }
								""".formatted(fx.teamId)))
				.andExpect(status().isCreated())
				.andReturn();
		String consentId = JsonPath.read(granted.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(get("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_CATEGORY"] }
								""".formatted(fx.teamId)))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/v1/athletes/me/transparency")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId())))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
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
								  "motivation": 2
								}
								""".formatted(today)))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + today)
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "baselineWindowDays": 7 }
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/training/readiness/daily/" + today)
						.with(ConsentHttpFixtures.accountAuth(fx.athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());
	}

	private void assertUnauthenticatedGetNever402(String path) throws Exception {
		mockMvc.perform(get(path))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.code").value(not(CommercialEntitlementRequiredException.CODE)));
	}

	private Fixture seedEntitledCoachTeam(String prefix) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		VerifiedAccount coach = accounts.registerVerified(prefix + "-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete(prefix + "-athlete");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		var subscription = OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(orgId), clock);
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
		ConsentHttpFixtures.acceptInvite(
				mockMvc,
				coach.accountId(),
				JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.rawToken"));
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");
		return new Fixture(owner, coach, athlete, orgId, teamId, athleteId, subscription);
	}

	private void expire(Fixture fx) {
		fx.subscription.expire(clock);
		subscriptionRepository.save(fx.subscription);
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

	private static String assignmentPath(Fixture fx) {
		return "/api/v1/teams/" + fx.teamId + "/athletes/" + fx.athleteId + "/training/assignments";
	}

	private static String overviewPath(Fixture fx) {
		return "/api/v1/teams/" + fx.teamId + "/athletes/" + fx.athleteId + "/overview";
	}

	private static String assignmentBody(String key, String title) {
		return """
				{
				  "title": "%s",
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
			com.devinolabs.uap.billing.domain.Subscription subscription) {
	}
}
