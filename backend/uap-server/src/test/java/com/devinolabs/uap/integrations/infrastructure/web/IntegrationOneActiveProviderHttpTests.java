package com.devinolabs.uap.integrations.infrastructure.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/**
 * Both OS hubs enabled so MVP one-active-provider HTTP conflict can be proven (ADR-048 D7).
 */
@SpringBootTest(properties = {
		"uap.integrations.enabled=true",
		"uap.integrations.apple-healthkit.enabled=true",
		"uap.integrations.health-connect.enabled=true"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IntegrationOneActiveProviderHttpTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Test
	void secondActiveProviderIsRejectedUntilDisconnect() throws Exception {
		AccountId accountId = athlete();
		UUID appleConnectionId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(appleConnectionId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lifecycleState").value("PENDING"));

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"HEALTH_CONNECT","requestId":"%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INTEGRATION_ACTIVE_CONNECTION_EXISTS"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + appleConnectionId + "/disconnect")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("DISCONNECTED"));

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"HEALTH_CONNECT","requestId":"%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.provider").value("HEALTH_CONNECT"))
				.andExpect(jsonPath("$.lifecycleState").value("PENDING"));
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey", "Nguyen", LocalDate.of(1995, 3, 12), Sex.MALE,
				Height.ofCentimeters(178), Weight.ofKilograms(74),
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

}
