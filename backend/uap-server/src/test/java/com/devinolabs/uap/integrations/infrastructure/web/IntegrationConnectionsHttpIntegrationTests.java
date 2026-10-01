package com.devinolabs.uap.integrations.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.devinolabs.uap.integrations.application.ConnectionRepository;
import com.devinolabs.uap.integrations.application.SyncRunRepository;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.integrations.enabled=true",
		"uap.integrations.apple-healthkit.enabled=true",
		"uap.integrations.health-connect.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IntegrationConnectionsHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private ConnectionRepository connectionRepository;

	@Autowired
	private SyncRunRepository syncRunRepository;

	@Test
	void unauthenticatedGetsAreUnauthorized() throws Exception {
		mockMvc.perform(get("/api/v1/integrations/connections"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void csrfIsRequiredOnMutations() throws Exception {
		AccountId accountId = athlete();
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(requestId)))
				.andExpect(status().isForbidden());
	}

	@Test
	void getListCreatesNothingAndForeignIdsAreUniform404() throws Exception {
		AccountId owner = athlete();
		AccountId stranger = athlete();

		long before = connectionRepository.countByAccountId(owner.value());
		mockMvc.perform(get("/api/v1/integrations/connections").with(auth(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$.length()").value(0));
		assertThat(connectionRepository.countByAccountId(owner.value())).isEqualTo(before);

		UUID connectionId = UUID.randomUUID();
		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(owner))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(connectionId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lifecycleState").value("PENDING"));

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId).with(auth(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/integrations/connections/" + UUID.randomUUID()).with(auth(owner)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
	}

	@Test
	void connectConfirmSyncDisconnectPipelineAndGetDoesNotSync() throws Exception {
		AccountId accountId = athlete();
		UUID connectionId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(connectionId)))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/confirm")
						.with(auth(accountId))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CONNECTED"))
				.andExpect(jsonPath("$.processConsentGranted").value(true));

		long syncRunsBeforeGet = syncRunRepository
				.findByConnectionIdOrderByRequestedAtDesc(ConnectionId.of(connectionId))
				.size();
		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId).with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CONNECTED"));
		assertThat(syncRunRepository.findByConnectionIdOrderByRequestedAtDesc(ConnectionId.of(connectionId)))
				.hasSize((int) syncRunsBeforeGet);

		UUID syncRequestId = UUID.randomUUID();
		MvcResult syncResult = mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/sync")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + syncRequestId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("FAILED"))
				.andExpect(jsonPath("$.errorCode").value("OS_HUB_UPLOAD_ONLY"))
				.andExpect(jsonPath("$.recordsAccepted").value(0))
				.andExpect(jsonPath("$.recordsRejected").value(0))
				.andReturn();
		assertThat(JsonPath.<String>read(syncResult.getResponse().getContentAsString(), "$.syncRunId"))
				.isEqualTo(syncRequestId.toString());

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId + "/sync-runs")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/disconnect")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("DISCONNECTED"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/sync")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INTEGRATION_SYNC_NOT_ACCEPTED"));
	}

	@Test
	void disabledProviderFailsClosed() throws Exception {
		AccountId accountId = athlete();

		mockMvc.perform(post("/api/v1/integrations/connections")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"HEALTH_CONNECT","requestId":"%s"}
								""".formatted(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INTEGRATION_PROVIDER_DISABLED"));
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
