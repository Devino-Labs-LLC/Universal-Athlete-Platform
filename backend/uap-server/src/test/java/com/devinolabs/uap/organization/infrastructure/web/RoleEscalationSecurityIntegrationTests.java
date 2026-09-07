package com.devinolabs.uap.organization.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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
class RoleEscalationSecurityIntegrationTests {

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
	void athletesAndHeadCoachesCannotEscalateBeyondInviteMatrix() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("esc-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("esc-athlete");
		VerifiedAccount head = accounts.registerVerified("esc-head");
		VerifiedAccount target = accounts.registerVerified("esc-target");

		String orgId = createOrg(owner.accountId(), "Esc Org");
		String teamId = createTeam(owner.accountId(), orgId, "Esc Team");

		String athleteToken = invite(owner.accountId(), teamId, athlete.email(), "ATHLETE");
		mockMvc.perform(post("/api/v1/invitations/" + athleteToken + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(target.email())))
				.andExpect(status().isNotFound());

		String headToken = invite(owner.accountId(), teamId, head.email(), "HEAD_COACH");
		mockMvc.perform(post("/api/v1/invitations/" + headToken + "/accept")
						.with(accountAuth(head.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(head.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "HEAD_COACH" }
								""".formatted(target.email())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(head.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(target.email())))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(accountAuth(head.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(target.email())))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_OWNER" }
								""".formatted(target.email())))
				.andExpect(status().isBadRequest());
	}

	private String invite(AccountId actor, String teamId, String email, String role) throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(actor))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "%s" }
								""".formatted(email, role)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
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
