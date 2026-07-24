package com.devinolabs.uap.athlete.infrastructure.web;

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
class AssessmentMeasurementHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void fullAttachmentLifecycleAuthCsrfReorderAndCompletionSnapshots() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String assessmentId = createAssessment(accountId, "Strength");
		String measurementId = createMeasurement(accountId, "80.0000");
		String measurementId2 = createMeasurement(accountId, "81.0000");

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(measurementId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult attached = mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementId":"%s",
								  "label":"Primary",
								  "notes":"baseline"
								}
								""".formatted(measurementId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.measurementId").value(measurementId))
				.andExpect(jsonPath("$.displayOrder").value(0))
				.andExpect(jsonPath("$.label").value("Primary"))
				.andExpect(jsonPath("$.snapshotted").value(false))
				.andExpect(jsonPath("$.value").value(80.0))
				.andExpect(jsonPath("$.athleteId").doesNotExist())
				.andExpect(jsonPath("$.version").doesNotExist())
				.andReturn();
		String attachmentId = JsonPath.read(attached.getResponse().getContentAsString(), "$.id");

		MvcResult attached2 = mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(measurementId2)))
				.andExpect(status().isCreated())
				.andReturn();
		String attachmentId2 = JsonPath.read(attached2.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(measurementId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_ASSESSMENT_MEASUREMENT"));

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)));

		mockMvc.perform(put("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/order")
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attachmentIds":["%s","%s"]}
								""".formatted(attachmentId2, attachmentId)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		mockMvc.perform(put("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attachmentIds":["%s","%s"]}
								""".formatted(attachmentId2, attachmentId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(attachmentId2))
				.andExpect(jsonPath("$[0].displayOrder").value(0))
				.andExpect(jsonPath("$[1].id").value(attachmentId))
				.andExpect(jsonPath("$[1].displayOrder").value(1));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/" + attachmentId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"label":null,"notes":"updated"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.label").value(nullValue()))
				.andExpect(jsonPath("$.notes").value("updated"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"START"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"COMPLETE"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("COMPLETED"));

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].snapshotted").value(true))
				.andExpect(jsonPath("$[0].snapshottedAt").isNotEmpty())
				.andExpect(jsonPath("$[0].value").value(81.0));

		mockMvc.perform(delete("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/" + attachmentId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ASSESSMENT_MEASUREMENT_MODIFICATION_NOT_ALLOWED"));

		mockMvc.perform(delete("/api/v1/athletes/me/measurements/" + measurementId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(2)))
				.andExpect(jsonPath("$[1].value").value(80.0));
	}

	@Test
	void validationErrorsCompletionWithoutMeasurementsAndCrossAccountNotFound() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		String assessmentId = createAssessment(accountId, "Empty Complete");
		String measurementId = createMeasurement(accountId, "70.0000");

		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"START"}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(patch("/api/v1/athletes/me/assessments/" + assessmentId + "/status")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"action":"COMPLETE"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("ASSESSMENT_COMPLETION_REQUIRES_MEASUREMENTS"));

		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(measurementId)))
				.andExpect(status().isCreated());

		mockMvc.perform(put("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/order")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"attachmentIds":["%s"]}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_ASSESSMENT_MEASUREMENT_ORDER"));

		AccountId other = AccountId.generate();
		createProfile(other);
		String otherMeasurementId = createMeasurement(other, "72.0000");
		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(otherMeasurementId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ATHLETE_MEASUREMENT_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements")
						.with(accountAuth(other)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSESSMENT_NOT_FOUND"));

		mockMvc.perform(delete("/api/v1/athletes/me/assessments/" + assessmentId + "/measurements/" + UUID.randomUUID())
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ASSESSMENT_MEASUREMENT_NOT_FOUND"));

		String otherAssessmentId = createAssessment(other, "Other");
		mockMvc.perform(post("/api/v1/athletes/me/assessments/" + otherAssessmentId + "/measurements")
						.with(accountAuth(other))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"measurementId":"%s"}
								""".formatted(otherMeasurementId)))
				.andExpect(status().isCreated());
		mockMvc.perform(delete("/api/v1/athletes/me/measurements/" + otherMeasurementId)
						.with(accountAuth(other))
						.with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ATHLETE_MEASUREMENT_IN_USE_BY_ASSESSMENT"));
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

	private String createAssessment(AccountId accountId, String title) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes/me/assessments")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "type":"STRENGTH",
								  "title":"%s"
								}
								""".formatted(title)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.id");
	}

	private String createMeasurement(AccountId accountId, String value) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/athletes/me/measurements")
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "measurementType":"BODY_WEIGHT",
								  "value":%s,
								  "unit":"KILOGRAM",
								  "measuredAt":"2026-07-20T10:00:00Z"
								}
								""".formatted(value)))
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
