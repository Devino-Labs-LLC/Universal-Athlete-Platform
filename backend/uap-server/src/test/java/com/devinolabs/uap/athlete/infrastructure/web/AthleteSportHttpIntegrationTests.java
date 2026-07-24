package com.devinolabs.uap.athlete.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class AthleteSportHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullSportLifecycleAndAuthRequirements() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(get("/api/v1/athletes/me/sports"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(soccerBody(true)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(soccerBody(true)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sportType").value("SOCCER"))
				.andExpect(jsonPath("$.primarySport").value(true))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.accountId").doesNotExist())
				.andReturn();
		String soccerId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sportType":"TENNIS",
								  "primarySport":false,
								  "participationLevel":"BEGINNER",
								  "yearsExperience":1,
								  "seasonStatus":"OFF_SEASON"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/athletes/me/sports").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[0].sportType").value("SOCCER"))
				.andExpect(jsonPath("$[0].primarySport").value(true));

		mockMvc.perform(patch("/api/v1/athletes/me/sports/" + soccerId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "participationLevel":"ADVANCED",
								  "preferredPosition":"Striker",
								  "yearsExperience":6,
								  "seasonStatus":"PRE_SEASON"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.participationLevel").value("ADVANCED"))
				.andExpect(jsonPath("$.preferredPosition").value("Striker"))
				.andExpect(jsonPath("$.sportType").value("SOCCER"));

		MvcResult tennisListed = mockMvc.perform(get("/api/v1/athletes/me/sports").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andReturn();
		String tennisId = JsonPath.read(tennisListed.getResponse().getContentAsString(), "$[1].id");

		mockMvc.perform(put("/api/v1/athletes/me/sports/" + tennisId + "/primary")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(tennisId))
				.andExpect(jsonPath("$.primarySport").value(true));

		mockMvc.perform(delete("/api/v1/athletes/me/sports/" + tennisId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/athletes/me/sports").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].primarySport").value(false));
	}

	@Test
	void validationDuplicateMissingProfileAndCrossAccountNotFound() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sportType":"RUNNING",
								  "primarySport":false,
								  "participationLevel":"BEGINNER",
								  "yearsExperience":100,
								  "seasonStatus":"YEAR_ROUND"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(soccerBody(false)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(soccerBody(false)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ATHLETE_SPORT"));

		AccountId missingProfile = AccountId.generate();
		mockMvc.perform(get("/api/v1/athletes/me/sports").with(accountAuth(missingProfile)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_NOT_FOUND"));

		AccountId other = AccountId.generate();
		createProfile(other);
		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sportType":"GOLF",
								  "primarySport":false,
								  "participationLevel":"RECREATIONAL",
								  "yearsExperience":1,
								  "seasonStatus":"YEAR_ROUND"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String sportId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(delete("/api/v1/athletes/me/sports/" + sportId)
						.with(accountAuth(other))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_SPORT_NOT_FOUND"));
	}

	private void createProfile(AccountId accountId) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
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
				.andExpect(status().isCreated());
	}

	private static String soccerBody(boolean primary) {
		return """
				{
				  "sportType":"SOCCER",
				  "primarySport":%s,
				  "participationLevel":"HIGH_SCHOOL",
				  "preferredPosition":"Forward",
				  "yearsExperience":4,
				  "seasonStatus":"IN_SEASON"
				}
				""".formatted(primary);
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
