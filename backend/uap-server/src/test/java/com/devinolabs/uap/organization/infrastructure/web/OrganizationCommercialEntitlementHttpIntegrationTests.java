package com.devinolabs.uap.organization.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.billing.application.EntitlementEnforcementProperties;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.billing.support.OrganizationSubscriptionFixtures;
import com.devinolabs.uap.consent.support.ConsentHttpFixtures;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.billing.entitlement-enforcement.enabled=true",
		"uap.billing.stripe.enabled=false"
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizationCommercialEntitlementHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private Environment environment;

	@Autowired
	private EntitlementEnforcementProperties enforcementProperties;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private Clock clock;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

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
	void enforcementTrueStartsWithoutStripeAndWithoutClientOverride() throws Exception {
		assertThat(enforcementProperties.isEnabled()).isTrue();
		assertThat(environment.getProperty("uap.billing.stripe.enabled")).isEqualTo("false");
		assertThat(environment.getProperty("uap.billing.stripe.secret-key", "")).isBlank();

		VerifiedAccount owner = accounts.registerVerified("c-flag-owner");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Flag Org");
		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.header("X-UAP-BILLING-ENTITLEMENT-ENFORCEMENT-ENABLED", "false")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Should Stay Gated" }
								"""))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
	}

	@Test
	void unauthenticatedAndInaccessibleNeverReturn402EvenWhenAnotherOrgIsPaid() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-authz-owner");
		VerifiedAccount stranger = accounts.registerVerified("c-authz-stranger");
		String unpaidOrg = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Unpaid AuthZ Org");
		VerifiedAccount paidOwner = accounts.registerVerified("c-authz-paid");
		String paidOrg = ConsentHttpFixtures.createOrg(mockMvc, paidOwner.accountId(), "Paid Foreign Org");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(paidOrg), clock);

		mockMvc.perform(post("/api/v1/organizations/" + unpaidOrg + "/teams")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Anon" }
								"""))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"))
				.andExpect(jsonPath("$.code").value(not(CommercialEntitlementRequiredException.CODE)));

		mockMvc.perform(post("/api/v1/organizations/" + unpaidOrg + "/teams")
						.with(ConsentHttpFixtures.accountAuth(stranger.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Stranger" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post("/api/v1/organizations/" + paidOrg + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Foreign paid" }
								"""))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
	}

	@Test
	void authorizedWithoutSubscriptionCreatesNoTeamAndReturnsCommercial402() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-nosub-owner");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "No Sub Org");
		Integer teamsBefore = jdbcTemplate.queryForObject("select count(*) from teams", Integer.class);

		MvcResult denied = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Gated Team" }
								"""))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE))
				.andExpect(jsonPath("$.message").exists())
				.andExpect(jsonPath("$.timestamp").exists())
				.andExpect(jsonPath("$.path").value("/api/v1/organizations/" + orgId + "/teams"))
				.andReturn();
		String body = denied.getResponse().getContentAsString();
		assertThat(body).doesNotContain("stripe", "price_", "cus_", "sub_", "sk_");
		assertThat(jdbcTemplate.queryForObject("select count(*) from teams", Integer.class)).isEqualTo(teamsBefore);
	}

	@Test
	void authorizedActiveSubscriptionCanCreateUpdateArchiveAndInvite() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-active-owner");
		VerifiedAccount invitee = accounts.registerVerified("c-active-admin");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Active Org");
		OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(orgId), clock);

		MvcResult created = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Active Team" }
								"""))
				.andExpect(status().isCreated())
				.andReturn();
		String teamId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(patch("/api/v1/teams/" + teamId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Renamed Team", "expectedVersion": 0 }
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Renamed Team"));

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(invitee.email())))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(invitee.email())))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	@Test
	void lifecycleStatesMatchEntitlementPortWithoutDuplicatingMath() throws Exception {
		assertCreateTeamForState("c-pending", SubscriptionLifecycleState.PENDING, null, null, null, 402);
		assertCreateTeamForState("c-expired", SubscriptionLifecycleState.EXPIRED, null, null, null, 402);
		assertCreateTeamForState("c-pastdue", SubscriptionLifecycleState.PAST_DUE, null, Instant.now(clock).plus(Duration.ofDays(7)), null, 402);
		assertCreateTeamForState("c-trial-ok", SubscriptionLifecycleState.TRIALING, Instant.now(clock).plus(Duration.ofDays(7)), null, null, 201);
		assertCreateTeamForState("c-trial-end", SubscriptionLifecycleState.TRIALING, Instant.now(clock), null, null, 402);
		assertCreateTeamForState(
				"c-cancel-ok",
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				null,
				Instant.now(clock).plus(Duration.ofDays(7)),
				null,
				201);
		assertCreateTeamForState(
				"c-cancel-end",
				SubscriptionLifecycleState.CANCEL_AT_PERIOD_END,
				null,
				Instant.now(clock),
				null,
				402);
		assertCreateTeamForState("c-grace-ok", SubscriptionLifecycleState.GRACE_PERIOD, null, null, Instant.now(clock).plus(Duration.ofDays(3)), 201);
		assertCreateTeamForState("c-grace-end", SubscriptionLifecycleState.GRACE_PERIOD, null, null, Instant.now(clock), 402);
	}

	@Test
	void individualPremiumOnOwnerDoesNotSatisfyOrganizationCapability() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-ind-owner");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Individual Org");
		OrganizationSubscriptionFixtures.saveIndividualPremium(
				subscriptionRepository, owner.accountId().value(), clock);

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Should 402" }
								"""))
				.andExpect(status().isPaymentRequired())
				.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
	}

	@Test
	void expiredSubscriptionDeniesMutationsWithoutSideEffects() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-side-owner");
		VerifiedAccount invitee = accounts.registerVerified("c-side-invitee");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Side Org");
		var subscription = OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(orgId), clock);
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Side Team");
		subscription.expire(clock);
		subscriptionRepository.save(subscription);

		mockMvc.perform(patch("/api/v1/teams/" + teamId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Mutated", "expectedVersion": 0 }
								"""))
				.andExpect(status().isPaymentRequired());
		mockMvc.perform(get("/api/v1/teams/" + teamId).with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.name").value("Side Team"))
				.andExpect(jsonPath("$.status").value("ACTIVE"))
				.andExpect(jsonPath("$.version").value(0));

		Integer invitationsBefore = jdbcTemplate.queryForObject("select count(*) from invitations", Integer.class);
		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(invitee.email())))
				.andExpect(status().isPaymentRequired());
		mockMvc.perform(post("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "COACH" }
								""".formatted(invitee.email())))
				.andExpect(status().isPaymentRequired());
		assertThat(jdbcTemplate.queryForObject("select count(*) from invitations", Integer.class))
				.isEqualTo(invitationsBefore);

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/archive")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isPaymentRequired());
		mockMvc.perform(get("/api/v1/teams/" + teamId).with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void freeControlSurfacesNeverReturn402WhenUnpaid() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("c-free-owner");
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("c-free-athlete");
		VerifiedAccount admin = accounts.registerVerified("c-free-admin");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), "Free Org");
		var subscription = OrganizationSubscriptionFixtures.saveActiveOrganization(
				subscriptionRepository, UUID.fromString(orgId), clock);
		String teamId = ConsentHttpFixtures.createTeam(mockMvc, owner.accountId(), orgId, "Free Team");

		MvcResult orgInvite = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(admin.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String orgToken = JsonPath.read(orgInvite.getResponse().getContentAsString(), "$.rawToken");
		VerifiedAccount pending = accounts.registerVerified("c-free-pending");
		MvcResult pendingInvite = mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ORG_ADMIN" }
								""".formatted(pending.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String pendingInviteId = JsonPath.read(pendingInvite.getResponse().getContentAsString(), "$.id");
		String athleteToken = ConsentHttpFixtures.inviteAthlete(mockMvc, owner.accountId(), teamId, athlete.email());

		subscription.expire(clock);
		subscriptionRepository.save(subscription);

		mockMvc.perform(get("/api/v1/organizations/" + orgId).with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + teamId).with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(patch("/api/v1/organizations/" + orgId)
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "Renamed Free Org", "expectedVersion": 0 }
								"""))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/organizations/" + orgId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + teamId + "/invitations")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/organizations/" + orgId + "/memberships")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + teamId + "/memberships")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/teams/" + teamId + "/roster")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId())))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/athletes/me/teams").with(ConsentHttpFixtures.accountAuth(athlete.accountId())))
				.andExpect(status().isOk());

		ConsentHttpFixtures.acceptInvite(mockMvc, athlete.accountId(), athleteToken);
		mockMvc.perform(post("/api/v1/invitations/" + orgToken + "/accept")
						.with(ConsentHttpFixtures.accountAuth(admin.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/invitations/" + pendingInviteId + "/revoke")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/v1/teams/" + teamId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(athlete.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/memberships/me/leave")
						.with(ConsentHttpFixtures.accountAuth(admin.accountId()))
						.with(csrf()))
				.andExpect(status().isNoContent());
	}

	private void assertCreateTeamForState(
			String prefix,
			SubscriptionLifecycleState state,
			Instant trialEndsAt,
			Instant currentPeriodEndsAt,
			Instant graceEndsAt,
			int expectedStatus) throws Exception {
		VerifiedAccount owner = accounts.registerVerified(prefix + "-owner");
		String orgId = ConsentHttpFixtures.createOrg(mockMvc, owner.accountId(), prefix + " Org");
		OrganizationSubscriptionFixtures.saveOrganizationState(
				subscriptionRepository,
				UUID.fromString(orgId),
				state,
				trialEndsAt,
				currentPeriodEndsAt,
				graceEndsAt,
				Instant.now(clock));
		mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
						.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "name": "%s Team" }
								""".formatted(prefix)))
				.andExpect(status().is(expectedStatus));
		if (expectedStatus == 402) {
			mockMvc.perform(post("/api/v1/organizations/" + orgId + "/teams")
							.with(ConsentHttpFixtures.accountAuth(owner.accountId()))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{ "name": "%s Team" }
									""".formatted(prefix)))
					.andExpect(jsonPath("$.code").value(CommercialEntitlementRequiredException.CODE));
		}
	}
}
