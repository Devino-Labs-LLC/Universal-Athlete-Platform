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
class AssessmentHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullAssessmentLifecycleAuthCsrfFiltersAndPatchSemantics() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String sportId = createSport(accountId);
		String goalId = createGoal(accountId, sportId);

		mockMvc.perform(get("/api/v1/athletes/me/assessments"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(assessmentBody("VERTICAL_JUMP", "Vertical", sportId, goalId, "2026-08-01T10:00:00Z")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(assessmentBody("VERTICAL_JUMP", "Vertical", sportId, goalId, "2026-08-01T10:00:00Z")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("VERTICAL_JUMP"))
				.andExpect(jsonPath("$.title").value("Vertical"))
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.athleteSportId").value(sportId))
				.andExpect(jsonPath("$.athleteGoalId").value(goalId))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String assessmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"Strength",
								  "scheduledAt":"2026-08-10T10:00:00Z"
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/athletes/me/assessments").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(get("/api/v1/athletes/me/assessments")
						.param("status", "PLANNED")
						.param("assessmentType", "VERTICAL_JUMP")
						.param("scheduledFrom", "2026-08-01T00:00:00Z")
						.param("scheduledTo", "2026-08-02T00:00:00Z")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].id").value(assessmentId));

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Vertical"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Vertical Baseline"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Vertical Baseline"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Vertical Baseline"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId)
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
				.andExpect(jsonPath("$.title").value("Vertical Baseline"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"START"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_PROGRESS"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"CANCEL"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELLED"));

		mockMvc.perform(delete("/api/v1/athletes/me/assessments/" + assessmentId)
						.with(accountAuth(accountId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(delete("/api/v1/athletes/me/assessments/" + assessmentId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void validationErrorsIsolationAndCrossAccountNotFound() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"OTHER",
								  "title":"Custom"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_CUSTOM_ASSESSMENT_TYPE"));

		mockMvc.perform(get("/api/v1/athletes/me/assessments")
						.param("scheduledFrom", "2026-08-10T00:00:00Z")
						.param("scheduledTo", "2026-08-01T00:00:00Z")
						.with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ASSESSMENT_DATE"));

		MvcResult created = mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"Strength",
								  "scheduledAt":"2026-08-01T10:00:00Z"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String assessmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"strength",
								  "scheduledAt":"2026-08-01T10:00:00Z"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ASSESSMENT"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"COMPLETE"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ASSESSMENT_STATUS"));

		AccountId missing = AccountId.generate();
		mockMvc.perform(get("/api/v1/athletes/me/assessments").with(accountAuth(missing)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_PROFILE_NOT_FOUND"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));

		String otherSportId = createSport(other);
		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"Linked",
								  "athleteSportId":"%s"
								}
								""".formatted(otherSportId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_SPORT_NOT_FOUND"));

		String otherGoalId = createGoal(other, otherSportId);
		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"Linked Goal",
								  "athleteGoalId":"%s"
								}
								""".formatted(otherGoalId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_GOAL_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"Missing Goal",
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
								  "sportType":"BASKETBALL",
								  "primarySport":true,
								  "participationLevel":"COLLEGIATE",
								  "yearsExperience":5,
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
								  "title":"Power",
								  "priority":"MEDIUM",
								  "athleteSportId":"%s"
								}
								""".formatted(sportId)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private static String assessmentBody(
			String type,
			String title,
			String sportId,
			String goalId,
			String scheduledAt) {
		return """
				{
				  "type":"%s",
				  "title":"%s",
				  "description":"baseline",
				  "notes":"bring shoes",
				  "scheduledAt":"%s",
				  "athleteSportId":"%s",
				  "athleteGoalId":"%s"
				}
				""".formatted(type, title, scheduledAt, sportId, goalId);
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
