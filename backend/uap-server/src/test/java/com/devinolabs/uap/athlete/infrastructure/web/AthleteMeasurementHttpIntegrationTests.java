package com.devinolabs.uap.athlete.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class AthleteMeasurementHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullMeasurementLifecycleAuthCsrfFiltersAndPatchSemantics() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String sportId = createSport(accountId);
		String goalId = createGoal(accountId, sportId);

		mockMvc.perform(get("/api/v1/athletes/me/measurements"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(weightBody(sportId, goalId, "80.0000", "2026-07-20T10:00:00Z")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(weightBody(sportId, goalId, "80.0000", "2026-07-20T10:00:00Z")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.measurementType").value("BODY_WEIGHT"))
				.andExpect(jsonPath("$.value").value(80.0))
				.andExpect(jsonPath("$.unit").value("KILOGRAM"))
				.andExpect(jsonPath("$.source").value("MANUAL"))
				.andExpect(jsonPath("$.athleteSportId").value(sportId))
				.andExpect(jsonPath("$.athleteGoalId").value(goalId))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String measurementId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"SESSION_RPE",
								  "value":7,
								  "unit":"SCORE",
								  "source":"COACH",
								  "measuredAt":"2026-07-21T10:00:00Z",
								  "athleteSportId":"%s"
								}
								""".formatted(sportId)))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/athletes/me/measurements").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/v1/athletes/me/measurements")
						.param("measurementType", "BODY_WEIGHT")
						.param("source", "MANUAL")
						.param("athleteSportId", sportId)
						.param("athleteGoalId", goalId)
						.param("measuredFrom", "2026-07-19T00:00:00Z")
						.param("measuredTo", "2026-07-21T00:00:00Z")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(measurementId));

		mockMvc.perform(get("/api/v1/athletes/me/measurements/" + measurementId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value(80.0));

		mockMvc.perform(patch("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"value":79.5}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(patch("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"value":79.5}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.value").value(79.5))
				.andExpect(jsonPath("$.unit").value("KILOGRAM"))
				.andExpect(jsonPath("$.athleteSportId").value(sportId));

		mockMvc.perform(patch("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "notes":null,
								  "athleteSportId":null,
								  "athleteGoalId":null
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.notes").value(nullValue()))
				.andExpect(jsonPath("$.athleteSportId").value(nullValue()))
				.andExpect(jsonPath("$.athleteGoalId").value(nullValue()))
				.andExpect(jsonPath("$.value").value(79.5));

		mockMvc.perform(delete("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(delete("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void validationErrorsIsolationAndCrossAccountNotFound() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T10:00:00Z"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":80,
								  "unit":"PERCENT",
								  "measuredAt":"2026-07-20T10:00:00Z"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MEASUREMENT_TYPE_UNIT_COMBINATION"));

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":80,
								  "unit":"KILOGRAM",
								  "measuredAt":"2099-01-01T00:00:00Z"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MEASUREMENT_TIMESTAMP"));

		mockMvc.perform(get("/api/v1/athletes/me/measurements")
						.param("measuredFrom", "2026-07-22T00:00:00Z")
						.param("measuredTo", "2026-07-20T00:00:00Z")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MEASUREMENT_DATE_RANGE"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":80,
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T10:00:00Z"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String measurementId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		AccountId missing = AccountId.generate();
		mockMvc.perform(get("/api/v1/athletes/me/measurements").with(accountAuth(missing)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_NOT_FOUND"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get("/api/v1/athletes/me/measurements/" + measurementId).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_MEASUREMENT_NOT_FOUND"));

		String otherSportId = createSport(other);
		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":81,
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T11:00:00Z",
								  "athleteSportId":"%s"
								}
								""".formatted(otherSportId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_SPORT_NOT_FOUND"));

		String otherGoalId = createGoal(other, otherSportId);
		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":81,
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T11:00:00Z",
								  "athleteGoalId":"%s"
								}
								""".formatted(otherGoalId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_GOAL_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":81,
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T11:00:00Z",
								  "athleteGoalId":"%s"
								}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_GOAL_NOT_FOUND"));
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

	private String createSport(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes/me/sports")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "sportType":"SOCCER",
								  "primarySport":true,
								  "participationLevel":"HIGH_SCHOOL",
								  "yearsExperience":4,
								  "seasonStatus":"IN_SEASON"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createGoal(AccountId accountId, String sportId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes/me/goals")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "goalType":"IMPROVE_STRENGTH",
								  "title":"Strength",
								  "priority":"MEDIUM",
								  "athleteSportId":"%s"
								}
								""".formatted(sportId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static String weightBody(String sportId, String goalId, String value, String measuredAt) {
		return """
				{
				  "measurementType":"BODY_WEIGHT",
				  "value":%s,
				  "unit":"KILOGRAM",
				  "notes":"check-in",
				  "measuredAt":"%s",
				  "athleteSportId":"%s",
				  "athleteGoalId":"%s"
				}
				""".formatted(value, measuredAt, sportId, goalId);
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
