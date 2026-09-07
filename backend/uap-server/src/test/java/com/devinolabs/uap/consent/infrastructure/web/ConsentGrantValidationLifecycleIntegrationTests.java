package com.devinolabs.uap.consent.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class ConsentGrantValidationLifecycleIntegrationTests {

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
	void emptyInvalidScopesArchivedTeamOrgAndLeftRemovedDenied() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("consent-val-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("consent-val-athlete");
		VerifiedAccount athlete2 = accounts.registerVerifiedAthlete("consent-val-athlete2");

		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Val Org");
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Val Team");
		String teamArchived = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Consent Val Archived");

		String token = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), token);

		String token2 = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete2.email());
		MvcResult accepted2 = ConsentHttpFixtures.acceptInvite(mockMvc, athlete2.accountId(), token2);
		String athlete2MembershipId = JsonPath.read(
				accepted2.getResponse().getContentAsString(),
				"$.teamMembership.id");

		String archivedToken = ConsentHttpFixtures.inviteAthlete(
				mockMvc, owner.accountId(), teamArchived, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), archivedToken);

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": [] }
								""".formatted(teamId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["NOT_A_REAL_SCOPE"] }
								""".formatted(teamId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/teams/" + teamArchived + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamArchived)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TEAM_ARCHIVED"));

		String orgArchivedId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Consent Val Org Archived");
		String teamInArchivedOrg = ConsentHttpFixtures.createTeam(
				mockMvc, owner.accountId(), orgArchivedId, "Team In Archived Org");
		String orgAthleteToken = ConsentHttpFixtures.inviteAthlete(
				mockMvc, owner.accountId(), teamInArchivedOrg, athlete.email());
		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), orgAthleteToken);
		mockMvc.perform(post("/api/v1/organizations/" + orgArchivedId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamInArchivedOrg)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_ARCHIVED"));

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamId)))
				.andExpect(status().isNotFound());

		mockMvc.perform(delete("/api/v1/teams/" + teamId + "/memberships/" + athlete2MembershipId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete2.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(teamId)))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/athletes/me/consents")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "teamId": "%s", "scopes": ["AVAILABILITY"] }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound());
	}

}
