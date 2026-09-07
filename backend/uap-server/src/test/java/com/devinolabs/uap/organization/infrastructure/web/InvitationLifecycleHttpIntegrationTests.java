package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class InvitationLifecycleHttpIntegrationTests {

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void createAcceptDeclineRevokeExpireWrongAccountInvalidTokenAndReplay() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("athlete");
		VerifiedAccount stranger = accounts.registerVerified("stranger");

		String organizationId = createOrg(owner.accountId(), "Invite Org");
		String teamId = createTeam(owner.accountId(), organizationId, "Invite Team");

		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.rawToken").isNotEmpty())
				.andExpect(jsonPath("$.invitedEmail").value(athlete.email()))
				.andReturn();

		String invitationId = JsonPath.read(invite.getResponse().getContentAsString(), "$.id");
		String rawToken = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");

		mockMvc.perform(get("/api/v1/teams/" + teamId + "/invitations").with(accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].rawToken").value(nullValue()));

		mockMvc.perform(post("/api/v1/invitations/not-a-real-token/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
						.with(accountAuth(stranger.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamMembership.role").value("ATHLETE"))
				.andExpect(jsonPath("$.teamMembership.athleteId").isNotEmpty());

		mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamMembership.accountId").value(athlete.accountId().value().toString()));

		VerifiedAccount coach = accounts.registerVerified("coach");
		MvcResult coachInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(coach.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String coachToken = JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.rawToken");
		String coachInvitationId = JsonPath.read(coachInvite.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/invitations/" + coachToken + "/decline")
						.with(accountAuth(coach.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/invitations/" + coachToken + "/accept")
						.with(accountAuth(coach.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));

		VerifiedAccount unverified = accounts.registerUnverified("unverified");
		MvcResult unverifiedInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(unverified.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String unverifiedToken = JsonPath.read(unverifiedInvite.getResponse().getContentAsString(), "$.rawToken");
		mockMvc.perform(post("/api/v1/invitations/" + unverifiedToken + "/accept")
						.with(accountAuth(unverified.accountId()))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_UNVERIFIED"));

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "not-an-email", "role": "ATHLETE" }
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		VerifiedAccount head = accounts.registerVerified("head");
		MvcResult headInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "HEAD_COACH" }
								""".formatted(head.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String headInvitationId = JsonPath.read(headInvite.getResponse().getContentAsString(), "$.id");
		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations/" + headInvitationId + "/revoke")
						.with(accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		String headToken = JsonPath.read(headInvite.getResponse().getContentAsString(), "$.rawToken");
		mockMvc.perform(post("/api/v1/invitations/" + headToken + "/accept")
						.with(accountAuth(head.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));

		VerifiedAccount expiredInvitee = accounts.registerVerifiedAthlete("expired");
		MvcResult expiredInvite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(expiredInvitee.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String expiredToken = JsonPath.read(expiredInvite.getResponse().getContentAsString(), "$.rawToken");
		String expiredInvitationId = JsonPath.read(expiredInvite.getResponse().getContentAsString(), "$.id");
		jdbcTemplate.update(
				"UPDATE invitations SET expires_at = ? WHERE invited_email = ?",
				java.sql.Timestamp.from(Instant.parse("2020-01-01T00:00:00Z")),
				expiredInvitee.email());

		mockMvc.perform(post("/api/v1/invitations/" + expiredToken + "/accept")
						.with(accountAuth(expiredInvitee.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound());

		VerifiedAccount orgAdmin = accounts.registerVerified("org-admin-life");
		MvcResult orgInvite = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(orgAdmin.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String orgInviteToken = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.rawToken");
		mockMvc.perform(post("/api/v1/invitations/" + orgInviteToken + "/accept")
						.with(accountAuth(orgAdmin.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.organizationMembership.role").value("ORG_ADMIN"));

		mockMvc.perform(get("/api/v1/me/invitations").with(accountAuth(athlete.accountId())))
				.andExpect(status().isOk());

		assert invitationId != null;
		assert coachInvitationId != null;
		assert expiredInvitationId != null;
	}

	private String createOrg(AccountId accountId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createTeam(AccountId accountId, String organizationId, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s" }
								""".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static RequestPostProcessor accountAuth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

}
