package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.devinolabs.uap.training.application.CoachAthleteOverviewResult.CoachOverviewSection;
import com.devinolabs.uap.training.application.CoachOverviewSectionStatus;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CoachAthleteOverviewUseCaseIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private GetCoachAthleteOverviewUseCase getCoachAthleteOverviewUseCase;

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
	void categoryDoesNotImplyScoreAndForeignViewerFailsClosed() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("uc-ov-owner");
		VerifiedAccount coach = accounts.registerVerified("uc-ov-coach");
		VerifiedAccount foreign = accounts.registerVerified("uc-ov-foreign");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("uc-ov-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "UC Overview Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "UC Overview Team");

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
		UUID athleteId = UUID.fromString(
				JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId"));
		UUID teamUuid = UUID.fromString(teamId);

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_CATEGORY"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		CoachAthleteOverviewResult overview = getCoachAthleteOverviewUseCase.execute(
				coach.accountId().value(),
				teamUuid,
				athleteId,
				null);

		assertThat(overview.effectiveScopes()).containsExactly("READINESS_CATEGORY");
		assertThat(overview.readinessCategory().status()).isEqualTo(CoachOverviewSectionStatus.NO_DATA);
		assertThat(overview.readinessScore().status()).isEqualTo(CoachOverviewSectionStatus.NOT_SHARED);
		assertThat(overview.limitingDimensions().status()).isEqualTo(CoachOverviewSectionStatus.NOT_SHARED);
		assertThat(overview.recoveryCheckIn().status()).isEqualTo(CoachOverviewSectionStatus.NOT_SHARED);
		assertThat(overview.availability()).isEqualTo(CoachOverviewSection.notShared());

		assertThatThrownBy(() -> getCoachAthleteOverviewUseCase.execute(
						foreign.accountId().value(),
						teamUuid,
						athleteId,
						null))
				.isInstanceOf(CoachAthleteOverviewNotFoundException.class);
	}

	@Test
	void storedReadinessProjectsAvailableSectionsAndScoreOmitsBand() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("uc-ov-ready-owner");
		VerifiedAccount coach = accounts.registerVerified("uc-ov-ready-coach");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("uc-ov-ready-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "UC Ready Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "UC Ready Team");

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
		UUID athleteId = UUID.fromString(
				JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId"));
		UUID teamUuid = UUID.fromString(teamId);
		LocalDate viewDate = LocalDate.now();

		seedStoredReadiness(athlete, viewDate);

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "teamId": "%s",
								  "scopes": ["AVAILABILITY", "READINESS_CATEGORY", "READINESS_SCORE",
								             "LIMITING_DIMENSIONS", "TRAINING_ADHERENCE", "PERFORMANCE_HISTORY"]
								}
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		CoachAthleteOverviewResult overview = getCoachAthleteOverviewUseCase.execute(
				coach.accountId().value(),
				teamUuid,
				athleteId,
				viewDate);

		assertThat(overview.availability().status()).isEqualTo(CoachOverviewSectionStatus.NO_DATA);
		assertThat(overview.trainingAdherence().status()).isEqualTo(CoachOverviewSectionStatus.NO_DATA);
		assertThat(overview.performanceHistory().status()).isEqualTo(CoachOverviewSectionStatus.NO_DATA);

		assertThat(overview.readinessCategory().status()).isEqualTo(CoachOverviewSectionStatus.AVAILABLE);
		assertThat(overview.readinessCategory().data().readinessBand()).isNotBlank();
		assertThat(overview.readinessCategory().data().dataSufficiency()).isNotBlank();

		assertThat(overview.readinessScore().status()).isEqualTo(CoachOverviewSectionStatus.AVAILABLE);
		assertThat(overview.readinessScore().data().readinessScore()).isNotNull();
		assertThat(overview.readinessScore().data().dataSufficiency()).isNotBlank();
		assertThat(overview.readinessScore().data().summaryReasonCode()).isNotBlank();

		assertThat(overview.limitingDimensions().status()).isEqualTo(CoachOverviewSectionStatus.AVAILABLE);
		assertThat(overview.limitingDimensions().data().limitingDimensions()).isNotNull();
	}

	private void seedStoredReadiness(VerifiedAccount athlete, LocalDate date) throws Exception {
		for (int daysBefore = 7; daysBefore >= 1; daysBefore--) {
			postRecoveryCheckIn(athlete, date.minusDays(daysBefore));
		}
		postRecoveryCheckIn(athlete, date);

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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readinessScore").isNumber());
	}

	private void postRecoveryCheckIn(VerifiedAccount athlete, LocalDate date) throws Exception {
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

}
