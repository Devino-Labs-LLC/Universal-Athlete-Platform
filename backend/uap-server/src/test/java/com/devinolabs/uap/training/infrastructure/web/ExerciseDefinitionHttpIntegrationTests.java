package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
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
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ExerciseDefinitionHttpIntegrationTests {

	private static final String BASE = "/api/v1/training/exercise-definitions";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void customDefinitionLifecycleIsAuthenticatedCsrfProtectedAndArchivedOnDelete() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(get(BASE))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"Sled Push"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"  Sled Push  "}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.scope").value("ATHLETE_CUSTOM"))
				.andExpect(jsonPath("$.canonicalName").value("Sled Push"))
				.andExpect(jsonPath("$.normalizedName").value("sled push"))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn();
		String definitionId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(BASE + "/" + definitionId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(definitionId))
				.andExpect(jsonPath("$.exercisePerformanceKey").value(definitionId));

		mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"sled   PUSH"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_EXERCISE_DEFINITION"));

		mockMvc.perform(patch(BASE + "/" + definitionId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"Heavy Sled Push"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(definitionId))
				.andExpect(jsonPath("$.canonicalName").value("Heavy Sled Push"));

		mockMvc.perform(get(BASE)
						.param("name", "sled")
						.param("scope", "ATHLETE_CUSTOM")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.definitions", hasSize(1)))
				.andExpect(jsonPath("$.definitions[0].id").value(definitionId));

		mockMvc.perform(delete(BASE + "/" + definitionId).with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(BASE).param("name", "sled").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.definitions", hasSize(0)));

		mockMvc.perform(get(BASE + "/" + definitionId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void systemSeedsAreListableButNotModifiableAndForeignCustomsAreHidden() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		AccountId other = AccountId.generate();
		createProfile(other);

		MvcResult foreign = mockMvc.perform(post(BASE)
						.with(accountAuth(other))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"Zercher Squat"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String foreignId = JsonPath.read(foreign.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(BASE).param("scope", "SYSTEM").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(8))
				.andExpect(jsonPath("$.definitions[0].scope").value("SYSTEM"));

		mockMvc.perform(patch(BASE + "/" + SystemExerciseDefinitions.BACK_SQUAT)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"Barbell Back Squat"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED"));

		mockMvc.perform(get(BASE + "/" + foreignId).with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXERCISE_DEFINITION_NOT_ACCESSIBLE"));

		mockMvc.perform(get(BASE + "/" + UUID.randomUUID()).with(accountAuth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("EXERCISE_DEFINITION_NOT_FOUND"));

		mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"A"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(get(BASE).param("size", "101").with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_EXERCISE_DEFINITION_QUERY"));
	}

	@Test
	void archivedDefinitionsCannotBePrescribed() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);

		MvcResult created = mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"canonicalName":"Sandbag Carry"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String definitionId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(delete(BASE + "/" + definitionId).with(accountAuth(accountId)).with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseDefinitionId":"%s",
								  "category":"STRENGTH",
								  "type":"BODYWEIGHT",
								  "sets":3
								}
								""".formatted(definitionId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EXERCISE_DEFINITION_ARCHIVED"));
	}

	@Test
	void prescriptionsDeriveTheirNameFromTheDefinitionAndExposeItsId() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);
		String dayId = createDay(accountId, planId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseDefinitionId":"%s",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3
								}
								""".formatted(SystemExerciseDefinitions.BACK_SQUAT)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.exerciseName").value("Back Squat"))
				.andExpect(jsonPath("$.exerciseDefinitionId")
						.value(SystemExerciseDefinitions.BACK_SQUAT.toString()));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"Tempo Front Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
	}

	private void createProfile(AccountId accountId) throws Exception {
		mockMvc.perform(post("/api/v1/athletes/me")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "firstName":"Robin",
								  "lastName":"Vega",
								  "dateOfBirth":"1994-03-08",
								  "sex":"FEMALE",
								  "heightCm":168.00,
								  "weightKg":63.00,
								  "dominantHand":"RIGHT",
								  "dominantFoot":"LEFT"
								}
								"""))
				.andExpect(status().isCreated());
	}

	private String createPlan(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"Definition Plan",
								  "startDate":"2026-06-01",
								  "endDate":"2026-08-31"
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createDay(AccountId accountId, String planId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Full Body","planWeekNumber":1,"scheduledDayOfWeek":"WEDNESDAY"}
								"""))
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
