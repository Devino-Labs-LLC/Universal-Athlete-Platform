package com.devinolabs.uap.athlete.infrastructure.web;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AthleteProfileHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createGetAndUpdateProfileThroughHttp() throws Exception {
		AccountId accountId = AccountId.generate();

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Jordan",
								  "lastName":"Lee",
								  "dateOfBirth":"1998-05-12",
								  "sex":"FEMALE",
								  "heightCm":175.00,
								  "weightKg":68.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"RIGHT"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.firstName").value("Jordan"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.dateOfBirth").value("1998-05-12"))
				.andExpect(jsonPath("$.id").exists())
				.andReturn();

		String athleteId = com.jayway.jsonpath.JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/athletes/me").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(athleteId))
				.andExpect(jsonPath("$.lastName").value("Lee"));

		mockMvc.perform(patch("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Alex",
								  "lastName":"Rivera",
								  "heightCm":180.00,
								  "weightKg":72.50,
								  "dominantHand":"LEFT",
								  "dominantFoot":"BOTH"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(athleteId))
				.andExpect(jsonPath("$.firstName").value("Alex"))
				.andExpect(jsonPath("$.lastName").value("Rivera"))
				.andExpect(jsonPath("$.heightCm").value(180.00))
				.andExpect(jsonPath("$.weightKg").value(72.50))
				.andExpect(jsonPath("$.dominantHand").value("LEFT"))
				.andExpect(jsonPath("$.dominantFoot").value("BOTH"))
				.andExpect(jsonPath("$.dateOfBirth").value("1998-05-12"))
				.andExpect(jsonPath("$.accountId").doesNotExist());
	}

	@Test
	void duplicateCreateReturnsConflict() throws Exception {
		AccountId accountId = AccountId.generate();
		String body = """
				{
				  "firstName":"Jordan",
				  "lastName":"Lee",
				  "dateOfBirth":"1998-05-12",
				  "sex":"FEMALE",
				  "heightCm":175.00,
				  "weightKg":68.00,
				  "dominantHand":"RIGHT",
				  "dominantFoot":"RIGHT"
				}
				""";

		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ATHLETE_PROFILE"));
	}

	@Test
	void unauthorizedAccessIsRejected() throws Exception {
		mockMvc.perform(get("/api/v1/athletes/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void validationErrorsReturnStandardPayload() throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(AccountId.generate()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"",
								  "lastName":"Lee",
								  "dateOfBirth":"1998-05-12",
								  "sex":"FEMALE",
								  "heightCm":10.00,
								  "weightKg":68.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"RIGHT"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.path").value("/api/v1/athletes/me"))
				.andExpect(jsonPath("$.details").isArray())
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
						.string(not(containsString("password"))));
	}

	@Test
	void usersOnlySeeTheirOwnProfile() throws Exception {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();

		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Jordan",
								  "lastName":"Lee",
								  "dateOfBirth":"1998-05-12",
								  "sex":"FEMALE",
								  "heightCm":175.00,
								  "weightKg":68.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"RIGHT"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/athletes/me").with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_NOT_FOUND"));
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
