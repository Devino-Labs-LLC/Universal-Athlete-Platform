package com.devinolabs.uap.training.infrastructure.web;

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

import java.time.LocalDate;
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
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrainingEnvironmentHttpIntegrationTests {

	private static final String BASE = "/api/v1/training/environments";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void environmentLifecycleIsAuthenticatedCsrfProtectedAndMapped() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);

		mockMvc.perform(get(BASE))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Home Gym",
								  "type":"HOME_GYM",
								  "availableEquipment":["DUMBBELL","BENCH"],
								  "defaultEnvironment":true
								}
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));

		MvcResult created = mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Home Gym",
								  "type":"HOME_GYM",
								  "availableEquipment":["DUMBBELL","BENCH"],
								  "defaultEnvironment":true
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Home Gym"))
				.andExpect(jsonPath("$.defaultEnvironment").value(true))
				.andReturn();
		String environmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get(BASE + "/" + environmentId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableEquipment", hasSize(2)));

		mockMvc.perform(get("/api/v1/training/exercise-definitions/"
						+ SystemExerciseDefinitions.BACK_SQUAT.value()
						+ "/environment-compatibility/" + environmentId)
						.with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.compatible").value(false))
				.andExpect(jsonPath("$.missingRequiredEquipment").isArray());

		mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"home gym",
								  "type":"HOME_GYM",
								  "availableEquipment":["DUMBBELL"]
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_TRAINING_ENVIRONMENT"));

		mockMvc.perform(patch(BASE + "/" + environmentId)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"availableEquipment":["DUMBBELL","PULL_UP_BAR"]}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.availableEquipment", hasSize(2)));

		mockMvc.perform(delete(BASE + "/" + environmentId)
						.with(accountAuth(accountId))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(get(BASE + "/" + environmentId).with(accountAuth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.active").value(false));
	}

	@Test
	void conflictingCandidateFiltersReturnStableError() throws Exception {
		AccountId accountId = AccountId.generate();
		createProfile(accountId);
		MvcResult created = mockMvc.perform(post(BASE)
						.with(accountAuth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "name":"Commercial Gym",
								  "type":"COMMERCIAL_GYM",
								  "availableEquipment":["BARBELL","SQUAT_RACK"]
								}
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String environmentId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(get("/api/v1/training/exercise-definitions/"
						+ SystemExerciseDefinitions.BACK_SQUAT.value()
						+ "/substitution-candidates")
						.param("equipment", EquipmentType.DUMBBELL.name())
						.param("trainingEnvironmentId", environmentId)
						.with(accountAuth(accountId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICTING_EQUIPMENT_CONTEXT_FILTERS"));
	}

	private void createProfile(AccountId accountId) throws Exception {
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
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
