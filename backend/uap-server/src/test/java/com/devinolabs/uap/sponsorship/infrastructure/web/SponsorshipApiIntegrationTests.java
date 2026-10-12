package com.devinolabs.uap.sponsorship.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

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
import com.devinolabs.uap.sponsorship.infrastructure.config.SponsorshipProperties;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SponsorshipApiIntegrationTests {

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
	private SponsorshipProperties sponsorshipProperties;

	private VerifiedAccountFixture fixture;

	@BeforeEach
	void setUp() {
		fixture = new VerifiedAccountFixture(
				registerAccountUseCase, verifyEmailUseCase, verificationNotifier, createAthleteProfileUseCase);
	}

	@Test
	void unauthenticatedPlacementsAreRejected() throws Exception {
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void athleteSeesNothingUntilAnActiveCampaignIsScheduled() throws Exception {
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("sponsor-empty");
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isNoContent());
	}

	@Test
	void nonOperatorAdminApisAreHidden() throws Exception {
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("sponsor-nonop");
		mockMvc.perform(get("/api/v1/platform/sponsorships/sponsors").with(asUser(athlete)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(athlete))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Brand\",\"displayName\":\"Brand\"}"))
				.andExpect(status().isNotFound());
	}

	@Test
	void operatorLifecycleEligibilityReportingAndCoachIsolation() throws Exception {
		VerifiedAccount operator = fixture.registerVerifiedAthlete("sponsor-op");
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("sponsor-ath");
		sponsorshipProperties.getOperatorAccountIds().add(operator.accountId().value());

		String createdSponsor = mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"name":"Acme Fuel","displayName":"Acme","websiteUrl":"https://acme.example/fuel","description":"Partner"}
						"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("DRAFT"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String sponsorId = com.jayway.jsonpath.JsonPath.read(createdSponsor, "$.id");

		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + sponsorId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));

		Instant start = Instant.now().minusSeconds(60);
		Instant end = Instant.now().plusSeconds(3600);
		String createdCampaign = mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sponsorId":"%s","name":"Fall hydration","status":"ACTIVE","startsAt":"%s","endsAt":"%s",
						"headline":"Hydrate with Acme","body":"Partner support for training days.","ctaLabel":"Learn more",
						"destinationUrl":"https://acme.example/offer","creativeUrl":"https://cdn.example/acme.png"}
						""".formatted(sponsorId, start, end)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String campaignId = com.jayway.jsonpath.JsonPath.read(createdCampaign, "$.id");

		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sponsorId":"%s","name":"Bad","status":"ACTIVE","startsAt":"%s",
						"headline":"Hi","body":"<script>x</script>","ctaLabel":"Go",
						"destinationUrl":"https://acme.example/offer"}
						""".formatted(sponsorId, start)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sponsorId":"%s","name":"Bad url","status":"ACTIVE","startsAt":"%s",
						"headline":"Hi","body":"Body","ctaLabel":"Go",
						"destinationUrl":"javascript:alert(1)"}
						""".formatted(sponsorId, start)))
				.andExpect(status().isBadRequest());

		String createdPlacement = mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/placements")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"surface\":\"HOME\",\"audience\":\"ALL\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String placementId = com.jayway.jsonpath.JsonPath.read(createdPlacement, "$.id");

		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.placementId").value(placementId))
				.andExpect(jsonPath("$.label").value("Sponsored"))
				.andExpect(jsonPath("$.headline").value("Hydrate with Acme"))
				.andExpect(jsonPath("$.sponsorDisplayName").value("Acme"));

		mockMvc.perform(post("/api/v1/sponsorships/placements/" + placementId + "/impressions")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/sponsorships/placements/" + placementId + "/impressions")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/sponsorships/placements/" + placementId + "/clicks")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/platform/sponsorships/reports").with(asUser(athlete)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/platform/sponsorships/reports").with(asUser(operator)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].impressions").value(1))
				.andExpect(jsonPath("$[0].clicks").value(1))
				.andExpect(jsonPath("$[0].clickThroughRate").value(1.0))
				.andExpect(jsonPath("$[0].accountId").doesNotExist())
				.andExpect(jsonPath("$[0].readiness").doesNotExist());

		mockMvc.perform(get("/api/v1/coach/summary").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facts[*].text", not(hasItem(containsString("Hydrate with Acme")))));

		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PAUSED\"}"))
				.andExpect(jsonPath("$.status").value("PAUSED"));
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(jsonPath("$.status").value("ACTIVE"));
		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ENDED\"}"))
				.andExpect(jsonPath("$.status").value("ENDED"));
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isNoContent());
	}

	@Test
	void futureCampaignIsNotClientActivatable() throws Exception {
		VerifiedAccount operator = fixture.registerVerifiedAthlete("sponsor-future-op");
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("sponsor-future-ath");
		sponsorshipProperties.getOperatorAccountIds().add(operator.accountId().value());
		String sponsor = mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Later\",\"displayName\":\"Later\"}"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String sponsorId = com.jayway.jsonpath.JsonPath.read(sponsor, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + sponsorId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"));
		Instant start = Instant.now().plusSeconds(86400);
		String campaign = mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sponsorId":"%s","name":"Later","status":"ACTIVE","startsAt":"%s",
						"headline":"Soon","body":"Not yet","ctaLabel":"Wait",
						"destinationUrl":"https://later.example/soon"}
						""".formatted(sponsorId, start)))
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String campaignId = com.jayway.jsonpath.JsonPath.read(campaign, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/placements")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"surface\":\"TRAINING\",\"audience\":\"ALL\"}"));
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "TRAINING").with(asUser(athlete)))
				.andExpect(status().isNoContent());
	}

	@Test
	void archivedSponsorHidesPlacements() throws Exception {
		VerifiedAccount operator = fixture.registerVerifiedAthlete("sponsor-arch-op");
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("sponsor-arch-ath");
		sponsorshipProperties.getOperatorAccountIds().add(operator.accountId().value());
		String sponsor = mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Gone\",\"displayName\":\"Gone\"}"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String sponsorId = com.jayway.jsonpath.JsonPath.read(sponsor, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + sponsorId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"));
		String campaign = mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"sponsorId":"%s","name":"Now","status":"ACTIVE","startsAt":"%s",
						"headline":"Hi","body":"Body copy","ctaLabel":"Go",
						"destinationUrl":"https://gone.example"}
						""".formatted(sponsorId, Instant.now().minusSeconds(10))))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String campaignId = com.jayway.jsonpath.JsonPath.read(campaign, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/campaigns/" + campaignId + "/placements")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"surface\":\"HOME\",\"audience\":\"ALL\"}"));
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + sponsorId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PAUSED\"}"));
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + sponsorId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ARCHIVED\"}"))
				.andExpect(jsonPath("$.status").value("ARCHIVED"));
		mockMvc.perform(get("/api/v1/sponsorships/placements").param("surface", "HOME").with(asUser(athlete)))
				.andExpect(status().isNoContent());
	}

	private RequestPostProcessor asUser(VerifiedAccount account) {
		AccountPrincipal principal = new AccountPrincipal(account.accountId());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal, null, principal.authorities());
		return authentication(authentication);
	}
}
