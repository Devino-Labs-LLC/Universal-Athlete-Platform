package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

}
