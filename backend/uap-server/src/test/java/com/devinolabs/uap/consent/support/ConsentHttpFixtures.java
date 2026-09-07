package com.devinolabs.uap.consent.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.jayway.jsonpath.JsonPath;

/**
 * Shared HTTP helpers for consent integration fixtures (org/team/invite flows).
 */
public final class ConsentHttpFixtures {

	private ConsentHttpFixtures() {
	}

	public static String createOrg(MockMvc mockMvc, AccountId accountId, String name) throws Exception {
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

	public static String createTeam(MockMvc mockMvc, AccountId accountId, String organizationId, String name)
			throws Exception {
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

	public static String inviteAthlete(MockMvc mockMvc, AccountId actor, String teamId, String email)
			throws Exception {
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(accountAuth(actor))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(email)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
	}

	public static MvcResult acceptInvite(MockMvc mockMvc, AccountId accountId, String rawToken) throws Exception {
		return mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();
	}

	public static RequestPostProcessor accountAuth(AccountId accountId) {
		AccountPrincipal principal = new AccountPrincipal(accountId);
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal,
				null,
				principal.authorities());
		return authentication(authentication);
	}

}
