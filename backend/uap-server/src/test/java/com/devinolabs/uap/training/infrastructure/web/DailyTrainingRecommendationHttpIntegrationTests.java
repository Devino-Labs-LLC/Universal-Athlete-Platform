package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
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
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, DailyTrainingRecommendationHttpIntegrationTests.MutableClockConfig.class })
class DailyTrainingRecommendationHttpIntegrationTests {

	private static final LocalDate STATE_DATE = LocalDate.of(2026, 7, 31);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void recommendationEndpointsRequireAuthAndReturnGuidanceOnly() throws Exception {
		AccountId accountId = athlete();

		mockMvc.perform(get("/api/v1/training/recommendations/history")
						.param("startDate", "2026-07-01")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/training/recommendations/daily/" + STATE_DATE)
						.with(auth(accountId))
						.with(csrf()))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("DAILY_TRAINING_RECOMMENDATION_READINESS_REQUIRED"));

		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "2026-07-31",
								  "sleepDurationMinutes": 360,
								  "sleepQuality": 2,
								  "fatigue": 5,
								  "muscleSoreness": 4,
								  "stress": 4,
								  "mood": 2,
								  "motivation": 2
								}
								"""))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + STATE_DATE)
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "baselineWindowDays": 7
								}
								"""))
				.andExpect(status().isOk());

		MvcResult readinessResult = mockMvc.perform(post("/api/v1/training/readiness/daily/" + STATE_DATE)
						.with(auth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andReturn();
		String assessmentId = JsonPath.read(readinessResult.getResponse().getContentAsString(), "$.assessmentId");

		mockMvc.perform(post("/api/v1/training/recommendations")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "dailyReadinessAssessmentId": "%s"
								}
								""".formatted(assessmentId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recommendationAlgorithmVersion").value("TRAINING_RECOMMENDATION_V1"))
				.andExpect(jsonPath("$.overallAction").exists())
				.andExpect(jsonPath("$.volumeReductionPercent").doesNotExist())
				.andExpect(jsonPath("$.recommendedAction").doesNotExist())
				.andExpect(jsonPath("$.medicalClearance").doesNotExist())
				.andExpect(jsonPath("$.appliedChanges").doesNotExist());

		mockMvc.perform(post("/api/v1/training/recommendations/daily/" + STATE_DATE)
						.with(auth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newlyCreated").value(false));

		mockMvc.perform(get("/api/v1/training/recommendations/history")
						.with(auth(accountId))
						.param("startDate", "2026-07-01")
						.param("endDate", "2026-07-31")
						.param("currentSnapshotOnly", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)));
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Avery", "Kim", LocalDate.of(1992, 11, 2), Sex.FEMALE,
				Height.ofCentimeters(165), Weight.ofKilograms(60),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(accountId),
				null,
				java.util.List.of());
		return authentication(authentication);
	}

	@TestConfiguration
	static class MutableClockConfig {

		@Bean
		@Primary
		Clock mutableClock() {
			return Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
		}

	}

}
