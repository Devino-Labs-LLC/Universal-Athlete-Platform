package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.ZoneId;

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
class TrainingScheduleHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void scheduleEndpointsRequireAuthenticationAndCsrf() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlanWithDay(accountId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(activationBody()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content(activationBody()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(get("/api/v1/training/calendar")
						.param("scheduledFrom", "2026-08-05")
						.param("scheduledTo", "2026-08-31"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/training/calendar/today").param("timezone", "UTC"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void activateGenerateRescheduleAndReadCalendar() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlanWithDay(accountId);
		String dayId = firstDayId(accountId, planId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(activationBody()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.plan.scheduleStatus").value("ACTIVE"))
				.andExpect(jsonPath("$.plan.recurrenceMode").value("FINITE"))
				.andExpect(jsonPath("$.plan.scheduleTimezone").value("Europe/Stockholm"))
				.andExpect(jsonPath("$.generation").doesNotExist());

		MvcResult generated = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/generate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledFrom":"2026-08-05","scheduledTo":"2026-08-18"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.createdCount").value(1))
				.andExpect(jsonPath("$.generatedThrough").value("2026-08-10"))
				.andExpect(jsonPath("$.createdOccurrences[0].scheduledDate").value("2026-08-10"))
				.andExpect(jsonPath("$.createdOccurrences[0].origin").value("GENERATED"))
				.andReturn();
		String occurrenceId = JsonPath.read(
				generated.getResponse().getContentAsString(), "$.createdOccurrences[0].id");

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId
						+ "/occurrences/" + occurrenceId + "/reschedule")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDate":"2026-08-11","plannedStartTime":"18:30:00"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduledDate").value("2026-08-11"))
				.andExpect(jsonPath("$.manuallyRescheduled").value(true))
				.andExpect(jsonPath("$.originalScheduledDate").value("2026-08-10"));

		mockMvc.perform(get("/api/v1/training/calendar")
						.with(accountAuth(accountId))
						.param("scheduledFrom", "2026-08-05")
						.param("scheduledTo", "2026-08-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].scheduledDate").value("2026-08-11"))
				.andExpect(jsonPath("$[0].trainingPlanName").value("Strength Plan"))
				.andExpect(jsonPath("$[0].workoutDayName").value("Lower Body"))
				.andExpect(jsonPath("$[0].exerciseCount").value(1))
				.andExpect(jsonPath("$[0].notStartedExerciseCount").value(1));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/pause")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduleStatus").value("PAUSED"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/resume")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduleStatus").value("ACTIVE"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/complete")
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.scheduleStatus").value("COMPLETED"));
	}

	@Test
	void todayResolvesTheRequestedTimezoneAndRejectsMissingOrInvalidOnes() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		createPlanWithDay(accountId);

		mockMvc.perform(get("/api/v1/training/calendar/today").with(accountAuth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

		mockMvc.perform(get("/api/v1/training/calendar/today")
						.with(accountAuth(accountId))
						.param("timezone", "Mars/Olympus"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

		mockMvc.perform(get("/api/v1/training/calendar/today")
						.with(accountAuth(accountId))
						.param("timezone", "Pacific/Auckland"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.timezone").value("Pacific/Auckland"))
				.andExpect(jsonPath("$.date").value(LocalDate.now(ZoneId.of("Pacific/Auckland")).toString()))
				.andExpect(jsonPath("$.entries", hasSize(0)));
	}

	@Test
	void surfacesStableErrorCodesForScheduleFailures() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlanWithDay(accountId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/generate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledFrom":"2026-08-05","scheduledTo":"2026-08-18"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_PLAN_SCHEDULE_STATUS"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "scheduleStartDate":"2026-08-05",
								  "timezone":"Not/AZone",
								  "recurrenceMode":"FINITE"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"timezone":"UTC"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(activationBody()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/generate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledFrom":"2026-08-05","scheduledTo":"2026-12-31"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_WORKOUT_OCCURRENCE_GENERATION_RANGE"));

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(activationBody()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_PLAN_SCHEDULE_STATUS"));

		mockMvc.perform(get("/api/v1/training/calendar")
						.with(accountAuth(accountId))
						.param("scheduledFrom", "2026-01-01")
						.param("scheduledTo", "2030-01-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_CALENDAR_RANGE"));
	}

	@Test
	void placementChangesAreLockedOnceOccurrencesExist() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlanWithDay(accountId);
		String dayId = firstDayId(accountId, planId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/schedule/activate")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "scheduleStartDate":"2026-08-05",
								  "timezone":"Europe/Stockholm",
								  "recurrenceMode":"FINITE",
								  "generateThrough":"2026-08-18"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.generation.createdCount").value(1));

		mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
						.patch("/api/v1/training/plans/" + planId + "/days/" + dayId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"scheduledDayOfWeek":"SUNDAY"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRAINING_PLAN_SCHEDULE_PLACEMENT_LOCKED"));
	}

	@Test
	void duplicatePlacementIsRejected() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String planId = createPlanWithDay(accountId);

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Second","planWeekNumber":1,"scheduledDayOfWeek":"MONDAY"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_WORKOUT_DAY_PLACEMENT"));
	}

	private static String activationBody() {
		return """
				{
				  "scheduleStartDate":"2026-08-05",
				  "timezone":"Europe/Stockholm",
				  "recurrenceMode":"FINITE"
				}
				""";
	}

	private String firstDayId(AccountId accountId, String planId) throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$[0].id");
	}

	private String createPlanWithDay(AccountId accountId) throws Exception {
		String planId = createPlan(accountId);
		MvcResult day = mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"title":"Lower Body","planWeekNumber":1,"scheduledDayOfWeek":"MONDAY"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String dayId = JsonPath.read(day.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/training/plans/" + planId + "/days/" + dayId + "/exercises")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "exerciseName":"Back Squat",
								  "category":"STRENGTH",
								  "type":"BARBELL",
								  "sets":3,
								  "minimumReps":5,
								  "maximumReps":5
								}
								"""))
				.andExpect(status().isCreated());
		return planId;
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
								  "endDate":"2026-12-31"
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
