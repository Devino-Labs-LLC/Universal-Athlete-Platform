package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

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
@Import({ TestcontainersConfiguration.class, RecoveryCheckInHttpIntegrationTests.MutableClockConfig.class })
class RecoveryCheckInHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void recoveryCheckInEndpointsRequireAuthAndSupportLifecycle() throws Exception {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey", "Lane", LocalDate.of(1995, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(75),
				DominantHand.RIGHT, DominantFoot.RIGHT);

		LocalDate checkInDate = LocalDate.of(2026, 7, 31);

		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "2026-07-31",
								  "sleepDurationMinutes": 420,
								  "sleepQuality": 3,
								  "fatigue": 4,
								  "muscleSoreness": 3,
								  "stress": 2,
								  "mood": 4,
								  "motivation": 3,
								  "discomfortAreas": [
								    {
								      "bodyArea": "LOWER_BACK",
								      "side": "RIGHT",
								      "intensity": 2,
								      "notes": "Mild tightness"
								    }
								  ],
								  "notes": "Tired"
								}
								"""))
				.andExpect(status().isUnauthorized());

		String createdJson = mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "2026-07-31",
								  "sleepDurationMinutes": 420,
								  "sleepQuality": 3,
								  "fatigue": 4,
								  "muscleSoreness": 3,
								  "stress": 2,
								  "mood": 4,
								  "motivation": 3,
								  "discomfortAreas": [
								    {
								      "bodyArea": "LOWER_BACK",
								      "side": "RIGHT",
								      "intensity": 2,
								      "notes": "Mild tightness"
								    }
								  ],
								  "notes": "Tired"
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fatigue.value").value(4))
				.andExpect(jsonPath("$.fatigue.label").value("HIGH"))
				.andExpect(jsonPath("$.completeness").value("COMPLETE"))
				.andReturn()
				.getResponse()
				.getContentAsString();

		String checkInId = createdJson.split("\"id\":\"")[1].split("\"")[0];

		mockMvc.perform(post("/api/v1/training/recovery-check-ins")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "checkInDate": "2026-07-31",
								  "fatigue": 3,
								  "muscleSoreness": 3,
								  "stress": 2,
								  "mood": 4,
								  "motivation": 3
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECOVERY_CHECK_IN_ALREADY_EXISTS"));

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/by-date/" + checkInDate).with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(checkInId));

		mockMvc.perform(patch("/api/v1/training/recovery-check-ins/" + checkInId)
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "fatigue": 3,
								  "motivation": 4,
								  "discomfortAreas": [],
								  "notes": "Updated"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fatigue.value").value(3))
				.andExpect(jsonPath("$.discomfortAreas").isEmpty());

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/" + checkInId + "/revisions").with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.revisions", hasSize(1)));

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/calendar")
						.param("startDate", "2026-07-27")
						.param("endDate", "2026-08-02")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days[?(@.date == '2026-07-31')].checkInPresent").value(true));

		mockMvc.perform(get("/api/v1/training/recovery-check-ins/history")
						.param("startDate", "2026-07-27")
						.param("endDate", "2026-08-02")
						.param("includeTrainingLoad", "false")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.days", hasSize(1)));
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(accountId),
				null,
				List.of());
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
