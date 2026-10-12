package com.devinolabs.uap.aicoach.infrastructure.web;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationRepository;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachMessageRepository;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AiCoachApiIntegrationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;
	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;
	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;
	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;
	@Autowired
	private CoachConversationRepository conversations;
	@Autowired
	private CoachMessageRepository messages;
	@Autowired
	private JdbcTemplate jdbcTemplate;

	private VerifiedAccountFixture accounts;

	@BeforeEach
	void setUp() {
		accounts = new VerifiedAccountFixture(
				registerAccountUseCase,
				verifyEmailUseCase,
				verificationNotifier,
				createAthleteProfileUseCase);
	}

	@Test
	void summaryIsReadOnlyAndMessagesFailClosedWhenAiIsDisabled() throws Exception {
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("coach-summary");
		long conversationsBefore = conversations.count();
		long snapshotsBefore = count("daily_athlete_state_snapshots");
		long readinessBefore = count("daily_readiness_assessments");
		long recommendationsBefore = count("daily_training_recommendations");
		long evidenceBefore = count("integration_evidence");

		mockMvc.perform(get("/api/v1/coach/summary"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/api/v1/coach/summary").with(accountAuth(athlete.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiEnabled").value(false))
				.andExpect(jsonPath("$.facts[?(@.topic == 'Readiness')].availability", hasItem("UNAVAILABLE")));

		org.junit.jupiter.api.Assertions.assertEquals(conversationsBefore, conversations.count());

		String created = mockMvc.perform(post("/api/v1/coach/conversations")
						.with(accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String conversationId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(post("/api/v1/coach/conversations/" + conversationId + "/messages")
						.with(accountAuth(athlete.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"text\":\"How am I looking today?\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
		org.junit.jupiter.api.Assertions.assertEquals(0, messages.count());
		org.junit.jupiter.api.Assertions.assertEquals(snapshotsBefore, count("daily_athlete_state_snapshots"));
		org.junit.jupiter.api.Assertions.assertEquals(readinessBefore, count("daily_readiness_assessments"));
		org.junit.jupiter.api.Assertions.assertEquals(recommendationsBefore, count("daily_training_recommendations"));
		org.junit.jupiter.api.Assertions.assertEquals(evidenceBefore, count("integration_evidence"));
	}

	private long count(String table) {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
		return count == null ? 0 : count;
	}

	@Test
	void conversationsDoNotCrossAthletes() throws Exception {
		VerifiedAccount owner = accounts.registerVerifiedAthlete("coach-owner");
		VerifiedAccount other = accounts.registerVerifiedAthlete("coach-other");
		String created = mockMvc.perform(post("/api/v1/coach/conversations")
						.with(accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String conversationId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(delete("/api/v1/coach/conversations/" + conversationId)
						.with(accountAuth(other.accountId()))
						.with(csrf()))
				.andExpect(status().isNotFound());

		mockMvc.perform(get("/api/v1/coach/conversations/current").with(accountAuth(other.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist());

		mockMvc.perform(get("/api/v1/coach/conversations/current").with(accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(conversationId));
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
