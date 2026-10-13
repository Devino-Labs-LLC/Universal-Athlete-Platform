package com.devinolabs.uap.marketplace.infrastructure.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
class MarketplaceApiIntegrationTests {

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
	void unauthenticatedMarketplaceIsRejected() throws Exception {
		mockMvc.perform(get("/api/v1/marketplace/listings")).andExpect(status().isUnauthorized());
	}

	@Test
	void nonOperatorAdminApisAreHidden() throws Exception {
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("mkt-nonop");
		mockMvc.perform(get("/api/v1/platform/marketplace/listings").with(asUser(athlete)))
				.andExpect(status().isNotFound());
		mockMvc.perform(post("/api/v1/platform/marketplace/listings")
				.with(csrf())
				.with(asUser(athlete))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"partnerId":"00000000-0000-0000-0000-000000000001","title":"Hack",
						"shortDescription":"x","fullDescription":"plain","listingType":"PRODUCT",
						"categoryCode":"TRAINING","status":"ACTIVE","audience":"ALL",
						"destinationUrl":"https://acme.example/x","startsAt":"%s"}
						""".formatted(Instant.now())))
				.andExpect(status().isNotFound());
	}

	@Test
	void operatorLifecycleDiscoverySavesOffersReportingAndIsolation() throws Exception {
		VerifiedAccount operator = fixture.registerVerifiedAthlete("mkt-op");
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("mkt-ath");
		VerifiedAccount other = fixture.registerVerifiedAthlete("mkt-other");
		sponsorshipProperties.getOperatorAccountIds().add(operator.accountId().value());

		String createdSponsor = mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Acme Gear\",\"displayName\":\"Acme Gear\"}"))
				.andExpect(status().isCreated())
				.andReturn()
				.getResponse()
				.getContentAsString();
		String partnerId = com.jayway.jsonpath.JsonPath.read(createdSponsor, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + partnerId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"));

		mockMvc.perform(get("/api/v1/marketplace/categories").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.code=='EQUIPMENT')].name").value("Equipment"));

		Instant start = Instant.now().minusSeconds(60);
		Instant end = Instant.now().plusSeconds(3600);
		String created = mockMvc.perform(post("/api/v1/platform/marketplace/listings")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"partnerId":"%s","title":"Recovery boots","shortDescription":"Pneumatic recovery",
						"fullDescription":"Partner recovery equipment for rest days.","listingType":"PRODUCT",
						"categoryCode":"RECOVERY","status":"ACTIVE","featured":true,"audience":"ALL",
						"destinationUrl":"https://acme.example/boots","priceDisplay":"$199",
						"discountText":"20 percent off","promoCode":"BOOTS20",
						"startsAt":"%s","endsAt":"%s","offerStartsAt":"%s","offerEndsAt":"%s"}
						""".formatted(partnerId, start, end, start, end)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String listingId = com.jayway.jsonpath.JsonPath.read(created, "$.id");

		mockMvc.perform(post("/api/v1/platform/marketplace/listings")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"partnerId":"%s","title":"Bad","shortDescription":"x","fullDescription":"<script>x</script>",
						"listingType":"PRODUCT","categoryCode":"TRAINING","status":"DRAFT","audience":"ALL",
						"destinationUrl":"https://acme.example/x","startsAt":"%s"}
						""".formatted(partnerId, start)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/platform/marketplace/listings")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"partnerId":"%s","title":"Bad url","shortDescription":"x","fullDescription":"plain",
						"listingType":"PRODUCT","categoryCode":"TRAINING","status":"DRAFT","audience":"ALL",
						"destinationUrl":"javascript:alert(1)","startsAt":"%s"}
						""".formatted(partnerId, start)))
				.andExpect(status().isBadRequest());

		mockMvc.perform(get("/api/v1/marketplace/listings").param("q", "boots").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.listings", hasSize(1)))
				.andExpect(jsonPath("$.listings[0].title").value("Recovery boots"))
				.andExpect(jsonPath("$.listings[0].partnerDisplayName").value("Acme Gear"))
				.andExpect(jsonPath("$.listings[0].promoCode").value("BOOTS20"))
				.andExpect(jsonPath("$.listings[0].readiness").doesNotExist());

		mockMvc.perform(get("/api/v1/marketplace/listings").param("category", "APPAREL").with(asUser(athlete)))
				.andExpect(jsonPath("$.listings", hasSize(0)));

		mockMvc.perform(get("/api/v1/marketplace/listings/" + listingId).with(asUser(athlete)))
				.andExpect(jsonPath("$.fullDescription").value("Partner recovery equipment for rest days."))
				.andExpect(jsonPath("$.offerActive").value(true));

		mockMvc.perform(put("/api/v1/marketplace/listings/" + listingId + "/save")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(get("/api/v1/marketplace/saves").with(asUser(other)))
				.andExpect(jsonPath("$", hasSize(0)));
		mockMvc.perform(get("/api/v1/marketplace/saves").with(asUser(athlete)))
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].saved").value(true));

		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/impressions")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/impressions")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/detail-views")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/clicks")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/redemptions")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/platform/marketplace/reports").with(asUser(athlete)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/platform/marketplace/reports").with(asUser(operator)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].impressions").value(1))
				.andExpect(jsonPath("$[0].detailViews").value(1))
				.andExpect(jsonPath("$[0].outboundClicks").value(1))
				.andExpect(jsonPath("$[0].redemptionInteractions").value(1))
				.andExpect(jsonPath("$[0].saves").value(1))
				.andExpect(jsonPath("$[0].accountId").doesNotExist())
				.andExpect(jsonPath("$[0].readiness").doesNotExist());

		mockMvc.perform(get("/api/v1/coach/summary").with(asUser(athlete)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.facts[*].text", not(hasItem(containsString("Recovery boots")))));

		mockMvc.perform(post("/api/v1/marketplace/listings/" + listingId + "/status")
				.with(csrf())
				.with(asUser(athlete))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/api/v1/platform/marketplace/listings/" + listingId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"PAUSED\"}"))
				.andExpect(jsonPath("$.status").value("PAUSED"));
		mockMvc.perform(get("/api/v1/marketplace/listings/" + listingId).with(asUser(athlete)))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/api/v1/marketplace/saves").with(asUser(athlete)))
				.andExpect(jsonPath("$[0].available").value(false))
				.andExpect(jsonPath("$[0].destinationUrl").value(org.hamcrest.Matchers.nullValue()))
				.andExpect(jsonPath("$[0].promoCode").value(org.hamcrest.Matchers.nullValue()));

		mockMvc.perform(delete("/api/v1/marketplace/listings/" + listingId + "/save")
				.with(csrf())
				.with(asUser(athlete)))
				.andExpect(status().isNoContent());
	}

	@Test
	void futureListingIsNotClientActivatable() throws Exception {
		VerifiedAccount operator = fixture.registerVerifiedAthlete("mkt-future-op");
		VerifiedAccount athlete = fixture.registerVerifiedAthlete("mkt-future-ath");
		sponsorshipProperties.getOperatorAccountIds().add(operator.accountId().value());
		String sponsor = mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Later Shop\",\"displayName\":\"Later Shop\"}"))
				.andReturn()
				.getResponse()
				.getContentAsString();
		String partnerId = com.jayway.jsonpath.JsonPath.read(sponsor, "$.id");
		mockMvc.perform(post("/api/v1/platform/sponsorships/sponsors/" + partnerId + "/status")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"status\":\"ACTIVE\"}"));
		Instant start = Instant.now().plusSeconds(86400);
		mockMvc.perform(post("/api/v1/platform/marketplace/listings")
				.with(csrf())
				.with(asUser(operator))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"partnerId":"%s","title":"Soon","shortDescription":"Not yet","fullDescription":"Wait",
						"listingType":"OFFER","categoryCode":"SERVICES","status":"ACTIVE","featured":false,
						"audience":"ALL","destinationUrl":"https://later.example/soon","startsAt":"%s"}
						""".formatted(partnerId, start)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status").value("SCHEDULED"));
		mockMvc.perform(get("/api/v1/marketplace/listings").with(asUser(athlete)))
				.andExpect(jsonPath("$.listings", hasSize(0)));
	}

	private RequestPostProcessor asUser(VerifiedAccount account) {
		AccountPrincipal principal = new AccountPrincipal(account.accountId());
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				principal, null, principal.authorities());
		return authentication(authentication);
	}
}
