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
class ConsentMembershipGenerationIntegrationTests {

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
	void grantBoundToMembershipGenerationBecomesIneffectiveAfterLeaveAndRejoinRequiresNewGrant() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-mem-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-mem-athlete");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Mem Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Mem Team");

		String token1 = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted1 = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token1);
		String membershipIdM1 = JsonPath.read(accepted1.getResponse().getContentAsString(), "$.teamMembership.id");
		String athleteId = JsonPath.read(accepted1.getResponse().getContentAsString(), "$.teamMembership.athleteId");

		MvcResult grant1 = mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_SCORE"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated())
				.andReturn();
		String boundMembership = JsonPath.read(grant1.getResponse().getContentAsString(), "$.teamMembershipId");
		assertThat(boundMembership).isEqualTo(membershipIdM1);
		assertThat(consentGrantsPort.hasEffectiveScope(
				UUID.fromString(athleteId),
				UUID.fromString(teamId),
				"READINESS_SCORE")).isTrue();

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		assertThat(consentGrantsPort.hasEffectiveScope(
				UUID.fromString(athleteId),
				UUID.fromString(teamId),
				"READINESS_SCORE")).isFalse();
		assertThat(consentGrantsPort.effectiveScopes(UUID.fromString(athleteId), UUID.fromString(teamId)))
				.isEmpty();

		String token2 = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		MvcResult accepted2 = ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token2);
		String membershipIdM2 = JsonPath.read(accepted2.getResponse().getContentAsString(), "$.teamMembership.id");
		assertThat(membershipIdM2).isNotEqualTo(membershipIdM1);

		assertThat(consentGrantsPort.hasEffectiveScope(
				UUID.fromString(athleteId),
				UUID.fromString(teamId),
				"READINESS_SCORE")).isFalse();

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["READINESS_SCORE"] }
								""".formatted(teamId)))
				.andExpect(status().isCreated())
				.andExpect(result -> {
					String newMembership = JsonPath.read(result.getResponse().getContentAsString(), "$.teamMembershipId");
					assertThat(newMembership).isEqualTo(membershipIdM2);
				});

		assertThat(consentGrantsPort.hasEffectiveScope(
				UUID.fromString(athleteId),
				UUID.fromString(teamId),
				"READINESS_SCORE")).isTrue();
	}

}
