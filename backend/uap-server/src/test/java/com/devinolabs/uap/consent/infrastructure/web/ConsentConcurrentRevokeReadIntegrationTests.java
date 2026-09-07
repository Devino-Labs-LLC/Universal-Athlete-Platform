package com.devinolabs.uap.consent.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.devinolabs.uap.consent.api.ConsentGrantsPort;
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
class ConsentConcurrentRevokeReadIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ConsentGrantsPort consentGrantsPort;

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
	void revokeThenPostCommitEffectiveQueryIsFalse() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-conc-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-conc-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Conc Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Conc Team");
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");

		MvcResult granted = mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["TRAINING_COLLABORATION"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated())
				.andReturn();
		String consentId = JsonPath.read(granted.getResponse().getContentAsString(), "$.id");

		UUID athleteUuid = UUID.fromString(athleteId);
		UUID teamUuid = UUID.fromString(teamId);
		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "TRAINING_COLLABORATION")).isTrue();

		mockMvc.perform(post("/api/v1/athletes/me/consents/" + consentId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "TRAINING_COLLABORATION")).isFalse();
		assertThat(consentGrantsPort.effectiveScopes(athleteUuid, teamUuid)).isEmpty();
	}

}
