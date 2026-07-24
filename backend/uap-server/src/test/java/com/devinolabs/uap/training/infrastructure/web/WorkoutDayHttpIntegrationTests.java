package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class WorkoutDayHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullWorkoutDayLifecycleAuthCsrfReorderAndStatus() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);

		mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lower Body","scheduledDay":"MONDAY"}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "title":"Lower Body",
								  "description":"Squats",
								  "scheduledDay":"MONDAY",
								  "plannedStartTime":"09:00:00",
								  "expectedDurationMinutes":60
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.title").value("Lower Body"))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andExpect(jsonPath("$.status").value("PLANNED"))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String dayId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		MvcResult second = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Upper Body","scheduledDay":"WEDNESDAY"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String dayId2 = JsonPath.read(second.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(put("/api/v1/training/plans/" + planId + "/days/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"dayIds":["%s","%s"]}
								""".formatted(dayId2, dayId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(dayId2))
				.andExpect(jsonPath("$[0].displayOrder").value(0))
				.andExpect(jsonPath("$[1].id").value(dayId))
				.andExpect(jsonPath("$[1].displayOrder").value(1));

		mockMvc.perform(patch("/api/v1/training/plans/" + planId + "/days/" + dayId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"description":null,"title":"Lower Body Power"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Lower Body Power"))
				.andExpect(jsonPath("$.description").value(nullValue()));

		mockMvc.perform(patch("/api/v1/training/plans/" + planId + "/days/" + dayId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"ACTIVATE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		mockMvc.perform(delete("/api/v1/training/plans/" + planId + "/days/" + dayId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("WORKOUT_DAY_DELETE_NOT_ALLOWED"));

		mockMvc.perform(delete("/api/v1/training/plans/" + planId + "/days/" + dayId2)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days").with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].displayOrder").value(0));
	}

	@Test
	void validationErrorsDuplicatesArchivedPlanAndCrossAccount() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlan(accountId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Missing day"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		MvcResult created = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lower Body","scheduledDay":"MONDAY"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String dayId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"lower body","scheduledDay":"TUESDAY"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_WORKOUT_DAY"));

		mockMvc.perform(put("/api/v1/training/plans/" + planId + "/days/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"dayIds":["%s"]}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_DAY_ORDER"));

		mockMvc.perform(patch("/api/v1/training/plans/" + planId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"ARCHIVE"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"New Day","scheduledDay":"FRIDAY"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_ARCHIVED"));

		AccountId other = AccountId.generate();
		createProfile(other);
		mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days/" + dayId).with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_NOT_FOUND"));
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

	private String createPlan(AccountId accountId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/training/plans")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "name":"Strength Plan",
								  "startDate":"2026-06-01",
								  "endDate":"2026-08-31"
								}
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
