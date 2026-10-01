package com.devinolabs.uap.integrations.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.servlet.ServletException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.connections.EvidenceBatchResultView;
import com.devinolabs.uap.integrations.api.connections.SyncRunView;
import com.devinolabs.uap.integrations.application.IntegrationConflictException;
import com.devinolabs.uap.integrations.application.IntegrationConnectionNotFoundException;
import com.devinolabs.uap.integrations.application.IntegrationConnectionService;
import com.devinolabs.uap.integrations.application.IntegrationEvidenceService;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class IntegrationConnectionsControllerTests {

	@Mock
	private IntegrationConnectionService connectionService;

	@Mock
	private IntegrationEvidenceService evidenceService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders
				.standaloneSetup(new IntegrationConnectionsController(connectionService, evidenceService))
				.setControllerAdvice(new IntegrationsExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
				.build();
	}

	@Test
	void listRequiresAuthenticatedAccountPrincipal() {
		assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/integrations/connections")).andReturn())
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.cause()
				.hasMessageContaining("Authenticated AccountPrincipal is required");
		verify(connectionService, never()).listConnectionsForAccount(any());
	}

	@Test
	void getMapsForeignOrMissingTo404() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID connectionId = UUID.randomUUID();
		when(connectionService.getConnection(accountId, connectionId))
				.thenThrow(new IntegrationConnectionNotFoundException());

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId).principal(authFor(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
	}

	@Test
	void beginConnectHappyPath() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(connectionService.beginConnect(accountId, requestId, HealthProviderKey.APPLE_HEALTHKIT))
				.thenReturn(connectionView(requestId));

		mockMvc.perform(post("/api/v1/integrations/connections")
						.principal(authFor(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"provider":"APPLE_HEALTHKIT","requestId":"%s"}
								""".formatted(requestId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.connectionId").value(requestId.toString()))
				.andExpect(jsonPath("$.lifecycleState").value("PENDING"))
				.andExpect(jsonPath("$.provider").value("APPLE_HEALTHKIT"));
	}

	@Test
	void syncMapsConflictWhenDisconnected() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID connectionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(connectionService.requestSync(accountId, connectionId, requestId))
				.thenThrow(new IntegrationConflictException(
						"INTEGRATION_SYNC_NOT_ACCEPTED",
						"Sync is only accepted while the connection is CONNECTED"));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/sync")
						.principal(authFor(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + requestId + "\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INTEGRATION_SYNC_NOT_ACCEPTED"));
	}

	@Test
	void listSyncRunsReturnsViews() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID connectionId = UUID.randomUUID();
		UUID syncRunId = UUID.randomUUID();
		when(connectionService.listSyncRuns(accountId, connectionId))
				.thenReturn(List.of(new SyncRunView(
						syncRunId,
						connectionId,
						"SUCCEEDED",
						Instant.parse("2026-09-30T16:00:00Z"),
						Instant.parse("2026-09-30T16:00:00Z"),
						Instant.parse("2026-09-30T16:00:00Z"),
						null,
						0,
						0)));

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId + "/sync-runs")
						.principal(authFor(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].syncRunId").value(syncRunId.toString()))
				.andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
	}

	@Test
	void evidenceBatchMapsResult() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID connectionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(evidenceService.uploadEvidenceBatch(any(), any(), any(), any()))
				.thenReturn(new EvidenceBatchResultView(requestId, requestId, 1, 0, false));

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.principal(authFor(accountId))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "requestId":"%s",
								  "items":[{
								    "externalRecordId":"hk-1",
								    "signalFamily":"SLEEP",
								    "signalType":"DURATION",
								    "valueNumeric":420,
								    "unitCode":"MINUTE",
								    "observedAt":"2026-09-29T06:00:00Z"
								  }]
								}
								""".formatted(requestId)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(0))
				.andExpect(jsonPath("$.replayed").value(false));
	}

	private static ConnectionView connectionView(UUID connectionId) {
		return new ConnectionView(
				connectionId,
				"APPLE_HEALTHKIT",
				"PENDING",
				false,
				null,
				null,
				null,
				null,
				null);
	}

	private static UsernamePasswordAuthenticationToken authFor(UUID accountId) {
		return new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(AccountId.of(accountId)),
				null,
				List.of());
	}

}
