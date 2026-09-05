package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

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
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationIdorSecurityIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

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
