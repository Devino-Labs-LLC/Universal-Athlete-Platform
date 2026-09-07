package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class OrganizationIdorSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
	void foreignOrgAndTeamAccessReturnsNotFoundWithoutExistenceLeak() throws Exception {
		AccountId owner = AccountId.generate();
		AccountId attacker = AccountId.generate();

		MvcResult ownedOrg = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Owned Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownedOrgId = JsonPath.read(ownedOrg.getResponse().getContentAsString(), "$.id");

		MvcResult attackerOrg = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Attacker Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String attackerOrgId = JsonPath.read(attackerOrg.getResponse().getContentAsString(), "$.id");

		MvcResult ownedTeam = mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/teams")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Owned Team" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownedTeamId = JsonPath.read(ownedTeam.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/organizations/" + ownedOrgId).with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(patch("/api/v1/organizations/" + ownedOrgId)
						.with(accountAuth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Hijack" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/archive")
						.with(accountAuth(attacker))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/organizations/" + ownedOrgId + "/teams").with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/teams")
						.with(accountAuth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Injected" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teams/" + ownedTeamId).with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(patch("/api/v1/teams/" + ownedTeamId)
						.with(accountAuth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Stolen" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/teams/" + ownedTeamId + "/archive")
						.with(accountAuth(attacker))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		// Org path / team mismatch: attacker's org path cannot create/list owned foreign team context.
		mockMvc.perform(get("/api/v1/organizations/" + attackerOrgId + "/teams").with(accountAuth(attacker)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		mockMvc.perform(get("/api/v1/teams/" + ownedTeamId).with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/organizations/" + UUID.randomUUID()).with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teams/" + UUID.randomUUID()).with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teams/" + ownedTeamId + "/invitations").with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/teams/" + ownedTeamId + "/memberships").with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/teams/" + ownedTeamId + "/invitations")
						.with(accountAuth(attacker))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "x@example.com", "role": "ATHLETE" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/organizations/" + ownedOrgId + "/invitations").with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/organizations/" + ownedOrgId + "/memberships").with(accountAuth(attacker)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
	}

	@Test
	void foreignDeleteMembershipAndRevokeInvitationReturnNotFound() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("idor-owner");
		VerifiedAccount attacker = accounts.registerVerified("idor-attacker");
		VerifiedAccount adminInvitee = accounts.registerVerified("idor-admin");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("idor-athlete");

		MvcResult ownedOrg = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "IDOR Owned Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownedOrgId = JsonPath.read(ownedOrg.getResponse().getContentAsString(), "$.id");

		MvcResult attackerOrg = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(attacker.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "IDOR Attacker Org" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String attackerOrgId = JsonPath.read(attackerOrg.getResponse().getContentAsString(), "$.id");

		MvcResult ownedTeam = mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/teams")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "IDOR Team" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownedTeamId = JsonPath.read(ownedTeam.getResponse().getContentAsString(), "$.id");

		MvcResult orgInvite = mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(adminInvitee.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String orgInvitationId = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.id");
		String orgInviteToken = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.rawToken");

		MvcResult acceptedAdmin = mockMvc.perform(post("/api/v1/invitations/" + orgInviteToken + "/accept")
						.with(accountAuth(adminInvitee.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();
		String adminMembershipId = JsonPath.read(
				acceptedAdmin.getResponse().getContentAsString(),
				"$.organizationMembership.id");

		MvcResult teamInvite = mockMvc.perform(post("/api/v1/teams/" + ownedTeamId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String teamInvitationId = JsonPath.read(teamInvite.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(delete("/api/v1/organizations/" + ownedOrgId + "/memberships/" + adminMembershipId)
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/organizations/" + attackerOrgId + "/memberships/" + adminMembershipId)
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/invitations/" + orgInvitationId + "/revoke")
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		// Create a fresh pending org invitation to revoke-IDOR after the accepted one is gone.
		MvcResult pendingOrgInvite = mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/invitations")
						.with(accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "pending+%s@example.com", "role": "ORG_ADMIN" }
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andReturn();
		String pendingOrgInvitationId = JsonPath.read(pendingOrgInvite.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/organizations/" + ownedOrgId + "/invitations/" + pendingOrgInvitationId
						+ "/revoke")
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/teams/" + ownedTeamId + "/invitations/" + teamInvitationId + "/revoke")
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/teams/" + ownedTeamId + "/memberships/" + UUID.randomUUID())
						.with(accountAuth(attacker.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TEAM_NOT_FOUND"));
	}

	@Test
	void unauthenticatedRequestsAreRejected() throws Exception {
		mockMvc.perform(get("/api/v1/organizations"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/organizations")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Nope" }
								"""))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/teams/" + UUID.randomUUID()))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void getDoesNotCreateOrganizationOrMembershipState() throws Exception {
		AccountId accountId = AccountId.generate();
		UUID missingOrgId = UUID.randomUUID();

		Integer orgCountBefore = jdbcTemplate.queryForObject(
				"select count(*) from organizations", Integer.class);
		Integer membershipCountBefore = jdbcTemplate.queryForObject(
				"select count(*) from organization_memberships", Integer.class);

		mockMvc.perform(get("/api/v1/organizations/" + missingOrgId).with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/organizations").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());

		Integer orgCountAfter = jdbcTemplate.queryForObject(
				"select count(*) from organizations", Integer.class);
		Integer membershipCountAfter = jdbcTemplate.queryForObject(
				"select count(*) from organization_memberships", Integer.class);

		assertThat(orgCountAfter).isEqualTo(orgCountBefore);
		assertThat(membershipCountAfter).isEqualTo(membershipCountBefore);
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
