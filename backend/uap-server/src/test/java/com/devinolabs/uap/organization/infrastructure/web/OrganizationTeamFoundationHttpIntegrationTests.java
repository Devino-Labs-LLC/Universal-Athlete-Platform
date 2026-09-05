package com.devinolabs.uap.organization.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationTeamFoundationHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createListGetUpdateArchiveOrganizationAndTeamsWithListIsolation() throws Exception {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();

		MvcResult createdOrg = mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Owner Org" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Owner Org"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.version").value(0))
				.andReturn();

		String organizationId = JsonPath.read(createdOrg.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/organizations")
						.with(accountAuth(other))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Other Org" }
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/organizations").with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(organizationId))
				.andExpect(jsonPath("$[0].name").value("Owner Org"));

		mockMvc.perform(get("/api/v1/organizations/" + organizationId).with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(organizationId));

		MvcResult updatedOrg = mockMvc.perform(patch("/api/v1/organizations/" + organizationId)
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Owner Org Renamed", "expectedVersion": 0 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Owner Org Renamed"))
				.andExpect(jsonPath("$.version").value(1))
				.andReturn();

		long orgVersion = ((Number) JsonPath.read(updatedOrg.getResponse().getContentAsString(), "$.version")).longValue();

		MvcResult createdTeam = mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Varsity" }
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Varsity"))
				.andExpect(jsonPath("$.organizationId").value(organizationId))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn();

		String teamId = JsonPath.read(createdTeam.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/organizations/" + organizationId + "/teams").with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(teamId));

		mockMvc.perform(get("/api/v1/teams/" + teamId).with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(teamId));

		mockMvc.perform(patch("/api/v1/teams/" + teamId)
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Varsity Elite", "expectedVersion": 0 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Varsity Elite"))
				.andExpect(jsonPath("$.version").value(1));

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/archive")
						.with(accountAuth(owner))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/teams/" + teamId).with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"));

		mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/archive")
						.with(accountAuth(owner))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/organizations/" + organizationId).with(accountAuth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ARCHIVED"))
				.andExpect(jsonPath("$.version").value(orgVersion + 1));

		mockMvc.perform(post("/api/v1/organizations/" + organizationId + "/teams")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Should Fail" }
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_ARCHIVED"));
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
