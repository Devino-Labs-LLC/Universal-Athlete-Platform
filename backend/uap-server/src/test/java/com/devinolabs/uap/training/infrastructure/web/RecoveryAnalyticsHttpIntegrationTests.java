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

@SpringBootTest
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, RecoveryAnalyticsHttpIntegrationTests.MutableClockConfig.class })
class RecoveryAnalyticsHttpIntegrationTests {

	private static final LocalDate CHECK_IN_DATE = LocalDate.of(2026, 7, 31);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void recoveryAnalyticsEndpointsRequireAuthAndReturnStructuredResponses() throws Exception {
		AccountId accountId = athlete();

		mockMvc.perform(get("/api/v1/training/recovery-analytics/dashboard")
						.param("baselineWindowDays", "7"))
				.andExpect(status().isUnauthorized());

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

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/by-date/" + CHECK_IN_DATE + "/baseline-comparison")
						.with(auth(accountId))
						.param("baselineWindowDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkInPresent").value(true))
				.andExpect(jsonPath("$.metricComparisons", hasSize(7)))
				.andExpect(jsonPath("$.metricComparisons[0].scaleDirection").exists())
				.andExpect(jsonPath("$.metricComparisons[0].comparisonBand").exists())
				.andExpect(jsonPath("$.metricComparisons[0].reasonCode").exists());

		mockMvc.perform(get("/api/v1/training/recovery-analytics/trends/FATIGUE")
						.with(auth(accountId))
						.param("startDate", "2026-07-24")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.metricType").value("FATIGUE"))
				.andExpect(jsonPath("$.scaleDirection").value("LOWER_REPORTED_VALUE"))
				.andExpect(jsonPath("$.trendDirection").exists())
				.andExpect(jsonPath("$.points[0].value.value").exists());

		mockMvc.perform(get("/api/v1/training/recovery-analytics/dashboard")
						.with(auth(accountId))
						.param("targetDate", "2026-07-31")
						.param("baselineWindowDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkInPresent").value(true))
				.andExpect(jsonPath("$.baselines", hasSize(7)))
				.andExpect(jsonPath("$.metricDeviations", hasSize(7)))
				.andExpect(jsonPath("$.metricTrends", hasSize(7)))
				.andExpect(jsonPath("$.readinessScore").doesNotExist())
				.andExpect(jsonPath("$.recommendation").doesNotExist());

		mockMvc.perform(get("/api/v1/training/recovery-analytics/discomfort-history")
						.with(auth(accountId))
						.param("startDate", "2026-07-24")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.observationCount").value(0));

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/by-date/2026-07-20/baseline-comparison")
						.with(auth(accountId))
						.param("baselineWindowDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkInPresent").value(false));

		mockMvc.perform(get("/api/v1/training/recovery-analytics/trends/INVALID")
						.with(auth(accountId))
						.param("startDate", "2026-07-24")
						.param("endDate", "2026-07-31"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_RECOVERY_METRIC_TYPE"));

		mockMvc.perform(get("/api/v1/training/recovery-analytics/dashboard")
						.with(auth(accountId))
						.param("baselineWindowDays", "10"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_RECOVERY_BASELINE_WINDOW"));
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey", "Lane", LocalDate.of(1995, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(75),
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
