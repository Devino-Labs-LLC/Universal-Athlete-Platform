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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.devinolabs.uap.integrations.application.EvidenceRepository;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.integrations.enabled=true",
		"uap.integrations.apple-healthkit.enabled=true",
		"uap.integrations.health-connect.enabled=false",
		"uap.integrations.backfill-days=30"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class IntegrationEvidenceHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private EvidenceRepository evidenceRepository;

	@Test
	void uploadHappyPathReplaySameRequestIdAndGetDoesNotWrite() throws Exception {
		AccountId accountId = athlete();
		UUID connectionId = connectAndConfirm(accountId);
		UUID requestId = UUID.randomUUID();

		String body = evidenceBatchBody(requestId, "hk-sleep-1", "420", "MINUTE", "2026-09-29T06:00:00Z");

		MvcResult first = mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(0))
				.andExpect(jsonPath("$.replayed").value(false))
				.andExpect(jsonPath("$.syncRunId").value(requestId.toString()))
				.andReturn();

		long countAfterFirst = evidenceRepository.countByConnectionId(ConnectionId.of(connectionId));
		assertThat(countAfterFirst).isEqualTo(1);

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(0))
				.andExpect(jsonPath("$.replayed").value(true))
				.andExpect(jsonPath("$.requestId")
						.value(JsonPath.<String>read(first.getResponse().getContentAsString(), "$.requestId")));

		assertThat(evidenceRepository.countByConnectionId(ConnectionId.of(connectionId))).isEqualTo(1);

		long beforeGet = evidenceRepository.countByConnectionId(ConnectionId.of(connectionId));
		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId + "/evidence")
						.param("from", "2026-09-01T00:00:00Z")
						.param("to", "2026-09-30T23:59:59Z")
						.param("family", "SLEEP")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].externalRecordId").value("hk-sleep-1"))
				.andExpect(jsonPath("$[0].signalFamily").value("SLEEP"));
		assertThat(evidenceRepository.countByConnectionId(ConnectionId.of(connectionId))).isEqualTo(beforeGet);
	}

	@Test
	void foreignConnectionIsUniform404() throws Exception {
		AccountId owner = athlete();
		AccountId stranger = athlete();
		UUID connectionId = connectAndConfirm(owner);
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(stranger))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(evidenceBatchBody(requestId, "x-1", "50", "BPM", "2026-09-29T06:00:00Z")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));

		mockMvc.perform(get("/api/v1/integrations/connections/" + connectionId + "/evidence")
						.param("from", "2026-09-01T00:00:00Z")
						.param("to", "2026-09-30T23:59:59Z")
						.with(auth(stranger)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("CONNECTION_NOT_FOUND"));
	}

	@Test
	void disconnectedRejectsIngest() throws Exception {
		AccountId accountId = athlete();
		UUID connectionId = connectAndConfirm(accountId);

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/disconnect")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(evidenceBatchBody(UUID.randomUUID(), "x-1", "50", "BPM", "2026-09-29T06:00:00Z")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INTEGRATION_INGEST_NOT_ACCEPTED"));
	}

	@Test
	void malformedItemsAreRejectedInCounts() throws Exception {
		AccountId accountId = athlete();
		UUID connectionId = connectAndConfirm(accountId);
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "requestId":"%s",
								  "items":[{
								    "externalRecordId":"bad-unit",
								    "signalFamily":"HEART",
								    "signalType":"RHR",
								    "valueNumeric":52,
								    "unitCode":"NOT_A_UNIT",
								    "observedAt":"2026-09-29T06:00:00Z"
								  },{
								    "externalRecordId":"bad-family",
								    "signalFamily":"BLOOD",
								    "signalType":"GLUCOSE",
								    "valueNumeric":5,
								    "unitCode":"COUNT",
								    "observedAt":"2026-09-29T06:00:00Z"
								  },{
								    "externalRecordId":"ok-hr",
								    "signalFamily":"HEART",
								    "signalType":"RHR",
								    "valueNumeric":52,
								    "unitCode":"BPM",
								    "observedAt":"2026-09-29T06:00:00Z"
								  }]
								}
								""".formatted(requestId)))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.acceptedCount").value(1))
				.andExpect(jsonPath("$.rejectedCount").value(2))
				.andExpect(jsonPath("$.replayed").value(false));

		assertThat(evidenceRepository.countByConnectionId(ConnectionId.of(connectionId))).isEqualTo(1);
	}

	@Test
	void concurrentUpsertSameExternalKeyEndsWithSingleRow() throws Exception {
		AccountId accountId = athlete();
		UUID connectionId = connectAndConfirm(accountId);
		String externalId = "concurrent-ext-1";

		ExecutorService pool = Executors.newFixedThreadPool(2);
		CountDownLatch start = new CountDownLatch(1);
		AtomicInteger acceptedHttp = new AtomicInteger();
		try {
			Future<?> a = pool.submit(() -> {
				start.await();
				postBatch(accountId, connectionId, UUID.randomUUID(), externalId, "400");
				acceptedHttp.incrementAndGet();
				return null;
			});
			Future<?> b = pool.submit(() -> {
				start.await();
				postBatch(accountId, connectionId, UUID.randomUUID(), externalId, "450");
				acceptedHttp.incrementAndGet();
				return null;
			});
			start.countDown();
			a.get(30, TimeUnit.SECONDS);
			b.get(30, TimeUnit.SECONDS);
		}
		finally {
			pool.shutdownNow();
		}

		assertThat(acceptedHttp.get()).isEqualTo(2);
		assertThat(evidenceRepository.countByConnectionId(ConnectionId.of(connectionId))).isEqualTo(1);
		assertThat(evidenceRepository
				.findByProviderAndAthleteIdAndExternalRecordId(
						HealthProviderKey.APPLE_HEALTHKIT,
						evidenceRepository
								.findByConnectionIdAndObservedAtBetween(
										ConnectionId.of(connectionId),
										java.time.Instant.parse("2026-09-01T00:00:00Z"),
										java.time.Instant.parse("2026-09-30T23:59:59Z"),
										null)
								.getFirst()
								.athleteId(),
						externalId))
				.isPresent();
	}

	private void postBatch(
			AccountId accountId,
			UUID connectionId,
			UUID requestId,
			String externalId,
			String value) throws Exception {
		mockMvc.perform(post("/api/v1/integrations/connections/" + connectionId + "/evidence-batches")
						.with(auth(accountId))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(evidenceBatchBody(requestId, externalId, value, "MINUTE", "2026-09-29T06:00:00Z")))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.acceptedCount").value(1));
	}

	private UUID connectAndConfirm(AccountId accountId) throws Exception {
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
				.andExpect(status().isOk());
		return connectionId;
	}

	private static String evidenceBatchBody(
			UUID requestId,
			String externalId,
			String value,
			String unit,
			String observedAt) {
		return """
				{
				  "requestId":"%s",
				  "items":[{
				    "externalRecordId":"%s",
				    "signalFamily":"SLEEP",
				    "signalType":"DURATION",
				    "valueNumeric":%s,
				    "unitCode":"%s",
				    "observedAt":"%s",
				    "provenanceClass":"CLIENT_DEVICE"
				  }]
				}
				""".formatted(requestId, externalId, value, unit, observedAt);
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
