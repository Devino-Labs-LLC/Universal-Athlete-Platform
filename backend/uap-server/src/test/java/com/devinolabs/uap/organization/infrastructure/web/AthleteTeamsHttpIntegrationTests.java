package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AthleteTeamsHttpIntegrationTests {

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
	void listsActiveAthleteTeamsAndExcludesNonAthleteAccounts() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("athlete");

		String organizationId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Picker Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), organizationId, "Picker Team");
		String rawToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), rawToken);

		mockMvc.perform(get("/api/v1/athletes/me/teams")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].teamId").value(teamId))
				.andExpect(jsonPath("$[0].teamName").value("Picker Team"))
				.andExpect(jsonPath("$[0].organizationName").value("Picker Org"))
				.andExpect(jsonPath("$[0].membershipId").isNotEmpty())
				.andExpect(jsonPath("$[0].athleteId").isNotEmpty());

		mockMvc.perform(get("/api/v1/athletes/me/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));

		mockMvc.perform(get("/api/v1/athletes/me/teams"))
				.andExpect(status().isUnauthorized());
	}

}
