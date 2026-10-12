package com.devinolabs.uap.aicoach.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CoachApiIntegrationTests {

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

	private VerifiedAccountFixture fixture;

	@BeforeEach
	void setUp() {
		fixture = new VerifiedAccountFixture(
				registerAccountUseCase, verifyEmailUseCase, verificationNotifier, createAthleteProfileUseCase);
	}

	@Test
	void summaryIsReadOnlyAndOmitsBodyMeasurements() throws Exception {
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("coach-summary");
		String body = mockMvc.perform(get("/api/v1/coach/summary").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiEnabled").value(false))
				.andExpect(jsonPath("$.facts[?(@.topic == 'Readiness')].availability", hasItem("UNAVAILABLE")))
				.andExpect(jsonPath("$.facts[?(@.topic == 'Profile')].text",
						hasItem(containsString("Body measurements are not included"))))
				.andExpect(jsonPath("$.facts[?(@.topic == 'Profile')].text", hasItem(containsString("Test"))))
				.andReturn()
				.getResponse()
				.getContentAsString();
		org.junit.jupiter.api.Assertions.assertFalse(body.contains("170"));
		org.junit.jupiter.api.Assertions.assertFalse(body.contains("\"65\""));
		org.junit.jupiter.api.Assertions.assertFalse(body.contains("2000-01-01"));
	}

	@Test
	void unauthenticatedSummaryIsRejected() throws Exception {
		mockMvc.perform(get("/api/v1/coach/summary")).andExpect(status().isUnauthorized());
	}

	@Test
	void conversationsStayInsideTheAthleteAndDoNotCallTheProviderWhenDisabled() throws Exception {
		VerifiedAccount owner = fixture.registerVerifiedAthlete("coach-owner");
		VerifiedAccount other = fixture.registerVerifiedAthlete("coach-other");
		String created = mockMvc.perform(post("/api/v1/coach/conversations").with(csrf()).with(asUser(owner)))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String conversationId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(delete("/api/v1/coach/conversations/" + conversationId).with(csrf()).with(asUser(other)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/coach/conversations/" + conversationId + "/messages")
				.with(csrf())
				.with(asUser(other))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"How am I looking today?\"}"))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/coach/conversations/" + conversationId + "/messages")
				.with(csrf())
				.with(asUser(owner))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"text\":\"How am I looking today?\"}"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
		mockMvc.perform(get("/api/v1/coach/conversations/current").with(asUser(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(conversationId))
				.andExpect(jsonPath("$.messages").isEmpty());
		mockMvc.perform(delete("/api/v1/coach/conversations").with(csrf()).with(asUser(owner)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/coach/conversations/current").with(asUser(owner)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").doesNotExist());
	}

	private RequestPostProcessor asUser(VerifiedAccount account) {
		AccountPrincipal principal = new AccountPrincipal(account.accountId());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal, null, principal.authorities());
		return authentication(authentication);
	}

}
