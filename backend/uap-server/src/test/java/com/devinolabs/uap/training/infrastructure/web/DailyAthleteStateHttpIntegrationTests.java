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
@Import({ TestcontainersConfiguration.class, DailyAthleteStateHttpIntegrationTests.MutableClockConfig.class })
class DailyAthleteStateHttpIntegrationTests {

	private static final LocalDate STATE_DATE = LocalDate.of(2026, 7, 31);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void athleteStateEndpointsRequireAuthAndSupportGenerateReadAndHistory() throws Exception {
		AccountId accountId = athlete();

		mockMvc.perform(get("/api/v1/training/athlete-state/daily/" + STATE_DATE))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/training/athlete-state/daily/" + STATE_DATE)
						.with(auth(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND"));

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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.snapshotVersion").value(1))
				.andExpect(jsonPath("$.current").value(true))
				.andExpect(jsonPath("$.changed").value(true))
				.andExpect(jsonPath("$.generationReason").value("MANUAL"))
				.andExpect(jsonPath("$.recovery.checkInPresent").value(true))
				.andExpect(jsonPath("$.recoveryMetrics", hasSize(7)))
				.andExpect(jsonPath("$.recoveryAnalyticsCalculationVersion").value("RECOVERY_ANALYTICS_V1"))
				.andExpect(jsonPath("$.readinessScore").doesNotExist())
				.andExpect(jsonPath("$.recommendation").doesNotExist());

		mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + STATE_DATE + "/regenerate")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "baselineWindowDays": 7
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.snapshotVersion").value(1))
				.andExpect(jsonPath("$.changed").value(false));

		mockMvc.perform(get("/api/v1/training/athlete-state/daily/" + STATE_DATE)
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.current").value(true));

		mockMvc.perform(get("/api/v1/training/athlete-state/daily/" + STATE_DATE + "/versions")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)));

		mockMvc.perform(get("/api/v1/training/athlete-state/history")
						.with(auth(accountId))
						.param("startDate", "2026-07-01")
						.param("endDate", "2026-07-31")
						.param("currentOnly", "true"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(1)))
				.andExpect(jsonPath("$.totalElements").value(1));

		mockMvc.perform(post("/api/v1/training/athlete-state/daily/" + STATE_DATE)
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "baselineWindowDays": 99
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_DAILY_ATHLETE_STATE_BASELINE_WINDOW"));
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Alex", "Morgan", LocalDate.of(1992, 5, 12), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(78),
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
