package com.devinolabs.uap.consent.api;

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
class ConsentGrantsPortContractTests {

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
	void portReportsEffectiveScopesAndRejectsUnknownScopeNames() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-port-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-port-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Port Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Port Team");
		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);
		String athleteId = JsonPath.read(accepted.getResponse().getContentAsString(), "$.teamMembership.athleteId");

		UUID athleteUuid = UUID.fromString(athleteId);
		UUID teamUuid = UUID.fromString(teamId);

		assertThat(consentGrantsPort.effectiveScopes(athleteUuid, teamUuid)).isEmpty();
		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "EXPORT")).isFalse();
		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "NOT_A_SCOPE")).isFalse();

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["EXPORT", "PERFORMANCE_HISTORY"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated());

		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "EXPORT")).isTrue();
		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "PERFORMANCE_HISTORY")).isTrue();
		assertThat(consentGrantsPort.hasEffectiveScope(athleteUuid, teamUuid, "AVAILABILITY")).isFalse();
		assertThat(consentGrantsPort.effectiveScopes(athleteUuid, teamUuid))
				.containsExactlyInAnyOrder("EXPORT", "PERFORMANCE_HISTORY");
	}

}
