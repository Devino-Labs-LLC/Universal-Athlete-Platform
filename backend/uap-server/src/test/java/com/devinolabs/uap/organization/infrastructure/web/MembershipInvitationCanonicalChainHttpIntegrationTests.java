package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
class MembershipInvitationCanonicalChainHttpIntegrationTests {

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
	void ownerCreatesTeamInvitesAthleteAcceptsListsMembershipAndMeInvitations() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("chain-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("chain-athlete");
		VerifiedAccount adminInvitee = accounts.registerVerified("chain-admin");

		MvcResult org = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Chain Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String orgId = JsonPath.read(org.getResponse().getContentAsString(), "$.id");

		MvcResult team = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Chain Team" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String teamId = JsonPath.read(team.getResponse().getContentAsString(), "$.id");

		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String invitationId = JsonPath.read(invite.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/me/invitations").with(accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(invitationId))
				.andExpect(jsonPath("$[0].organizationName").value("Chain Org"))
				.andExpect(jsonPath("$[0].teamName").value("Chain Team"));

		mockMvc.perform(post("/api/v1/me/invitations/" + invitationId + "/accept")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.teamMembership.role").value("ATHLETE"));

		mockMvc.perform(get("/api/v1/teams/" + teamId + "/memberships").with(accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].accountId").value(athlete.accountId().value().toString()));

		MvcResult orgInvite = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(adminInvitee.email())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.role").value("ORG_ADMIN"))
				.andReturn();
		String adminInvitationId = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(adminInvitee.email())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PENDING_INVITATION_EXISTS"));

		mockMvc.perform(get("/api/v1/me/invitations").with(accountAuth(adminInvitee.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(adminInvitationId))
				.andExpect(jsonPath("$[0].organizationName").value("Chain Org"))
				.andExpect(jsonPath("$[0].teamName").value(nullValue()));

		mockMvc.perform(post("/api/v1/me/invitations/" + adminInvitationId + "/accept")
						.with(accountAuth(adminInvitee.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.organizationMembership.role").value("ORG_ADMIN"));

		mockMvc.perform(get("/api/v1/organizations/" + orgId + "/memberships")
						.with(accountAuth(adminInvitee.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/v1/organizations/" + orgId + "/invitations")
						.with(accountAuth(adminInvitee.accountId())))
				.andExpect(status().isOk());
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
