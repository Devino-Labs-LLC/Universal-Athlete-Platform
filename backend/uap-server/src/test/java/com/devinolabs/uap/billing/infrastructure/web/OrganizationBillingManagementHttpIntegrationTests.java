package com.devinolabs.uap.billing.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.nullValue;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.identity.application.RegisterAccountUseCase;
import com.devinolabs.uap.identity.application.VerifyEmailUseCase;
import com.devinolabs.uap.identity.infrastructure.notification.InMemoryVerificationNotifier;
import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.application.SecurityAuditEventRepository;
import com.devinolabs.uap.billing.application.BillingConflictException;
import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.OrganizationBillingCustomerRepository;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.BillingSubject;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.OrganizationBillingCustomer;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.Subscription;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.application.TeamMembershipRepository;
import com.devinolabs.uap.organization.application.TeamRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.Team;
import com.devinolabs.uap.organization.domain.TeamId;
import com.devinolabs.uap.organization.domain.TeamMembership;
import com.devinolabs.uap.organization.domain.TeamMembershipId;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture;
import com.devinolabs.uap.organization.support.VerifiedAccountFixture.VerifiedAccount;
import com.jayway.jsonpath.JsonPath;

@SpringBootTest(properties = {
		"uap.billing.entitlement-enforcement.enabled=true",
		"uap.billing.stripe.enabled=true",
		"uap.billing.stripe.secret-key=rk_test_placeholder_for_management_tests",
		"uap.billing.stripe.webhook-secret=whsec_placeholder_for_management_tests",
		"uap.billing.stripe.success-url=https://app.example.com/billing/success?session_id={CHECKOUT_SESSION_ID}",
		"uap.billing.stripe.cancel-url=https://app.example.com/billing/cancel",
		"uap.billing.stripe.portal-configuration-id=bpc_test_management",
		"uap.billing.stripe.portal-return-url=https://app.example.com/coach/billing",
		"uap.billing.stripe.prices.org-band-25-monthly=price_mgmt_25_monthly",
		"uap.billing.stripe.prices.org-band-25-annual=price_mgmt_25_annual",
		"uap.billing.stripe.prices.org-band-75-monthly=price_mgmt_75_monthly",
		"uap.billing.stripe.prices.org-band-75-annual=price_mgmt_75_annual",
		"uap.billing.stripe.prices.org-band-250-monthly=price_mgmt_250_monthly",
		"uap.billing.stripe.prices.org-band-250-annual=price_mgmt_250_annual"
})
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, OrganizationBillingManagementHttpIntegrationTests.ProviderConfig.class })
class OrganizationBillingManagementHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private TeamRepository teamRepository;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private OrganizationBillingCustomerRepository customerRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Autowired
	private TeamMembershipRepository teamMembershipRepository;

	@Autowired
	private SecurityAuditEventRepository auditRepository;

	@Autowired
	private RegisterAccountUseCase registerAccountUseCase;

	@Autowired
	private VerifyEmailUseCase verifyEmailUseCase;

	@Autowired
	private InMemoryVerificationNotifier verificationNotifier;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private Clock clock;

	private VerifiedAccountFixture accounts;

	@BeforeEach
	void resetProvider() {
		ManagementProvider.reset();
		accounts = new VerifiedAccountFixture(
				registerAccountUseCase,
				verifyEmailUseCase,
				verificationNotifier,
				createAthleteProfileUseCase);
	}

	@Test
	void portalIsOwnerOnlyAndUsesTheStoredCustomer() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		customerRepository.save(OrganizationBillingCustomer.stripe(organizationId, "cus_stored", Instant.now(clock)));
		UUID adminId = membership(organizationId, OrganizationMembershipRole.ORG_ADMIN);
		TeamId teamId = team(ownerId, organizationId);
		UUID coachId = teamRole(teamId, OrganizationMembershipRole.COACH, null);
		UUID headCoachId = teamRole(teamId, OrganizationMembershipRole.HEAD_COACH, null);
		UUID teamAdminId = teamRole(teamId, OrganizationMembershipRole.TEAM_ADMIN, null);
		UUID athleteId = teamRole(teamId, OrganizationMembershipRole.ATHLETE, UUID.randomUUID());

		for (UUID actor : List.of(adminId, coachId, headCoachId, teamAdminId, athleteId, UUID.randomUUID())) {
			mockMvc.perform(post(portalPath(organizationId))
							.with(authentication(authFor(actor)))
							.with(csrf()))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
		}
		assertThat(ManagementProvider.portalCalls).isZero();

		mockMvc.perform(post(portalPath(organizationId)).with(csrf()))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(portalPath(organizationId)).with(authentication(authFor(ownerId))))
				.andExpect(status().isForbidden());

		mockMvc.perform(post(portalPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://billing.stripe.test/portal/" + organizationId))
				.andExpect(jsonPath("$.configurationId").doesNotExist());
		assertThat(ManagementProvider.portalCustomer).isEqualTo("cus_stored");
		assertThat(auditTypes(organizationId)).noneMatch(type -> type.startsWith("BILLING_"));
	}

	@Test
	void undersizedCheckoutDoesNotCallTheProvider() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		TeamId teamId = team(ownerId, organizationId);
		seedAthletes(teamId, 40);

		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(UUID.randomUUID(), "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_PLAN_CAPACITY_CONFLICT"));
		assertThat(ManagementProvider.checkoutCalls).isZero();
		assertThat(ManagementProvider.customerCalls).isZero();

		seedAthletes(teamId, 60);
		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(UUID.randomUUID(), "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isConflict());
		seedAthletes(teamId, 151);
		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(UUID.randomUUID(), "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isConflict());
		assertThat(ManagementProvider.checkoutCalls).isZero();
	}

	@Test
	void checkoutActivationAboveTheBandStaysProviderTruthAndOverCapacity() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		TeamId teamId = team(ownerId, organizationId);
		seedAthletes(teamId, 20);
		UUID requestId = UUID.randomUUID();
		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(requestId, "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isCreated());
		seedAthletes(teamId, 6);

		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhookPayload(organizationId, requestId, "evt_checkout_race")))
				.andExpect(status().isOk());

		assertThat(subscriptionRepository.findById(SubscriptionId.of(requestId))).get().satisfies(subscription -> {
			assertThat(subscription.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_25);
			assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.TRIALING);
		});
		mockMvc.perform(get("/api/v1/billing/organizations/" + organizationId + "/capacity")
						.with(authentication(authFor(ownerId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeAthleteCount").value(26))
				.andExpect(jsonPath("$.overCapacity").value(true));
		assertThat(ManagementProvider.restoreCalls).isZero();
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(OrganizationId.of(organizationId))).isEqualTo(26);
	}

	@Test
	void ownerCanUpgradeOnTheSameSubscriptionAndAuditOnce() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(requestId, "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscription.id().value().toString()))
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_75"))
				.andExpect(jsonPath("$.providerSubscriptionRef").doesNotExist());

		assertThat(subscriptionRepository.findBySubject(
				com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION, organizationId)).hasSize(1);
		assertThat(ManagementProvider.planChangeCalls).isEqualTo(1);
		assertThat(auditTypes(organizationId)).filteredOn(type -> type.equals("BILLING_PLAN_CHANGED")).hasSize(1);

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(requestId, "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isOk());
		assertThat(ManagementProvider.planChangeCalls).isEqualTo(1);
		assertThat(auditTypes(organizationId)).filteredOn(type -> type.equals("BILLING_PLAN_CHANGED")).hasSize(1);
	}

	@Test
	void paymentFailureAndProviderOutageDoNotChangeThePlan() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		ManagementProvider.paymentNotApplied = true;

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "ANNUAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_PAYMENT_NOT_APPLIED"));
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_25);
		assertThat(auditTypes(organizationId)).noneMatch("BILLING_PLAN_CHANGED"::equals);

		ManagementProvider.paymentNotApplied = false;
		ManagementProvider.unavailable = true;
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "ANNUAL")))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("BILLING_PROVIDER_UNAVAILABLE"));
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_25);
	}

	@Test
	void downgradeAtTheBoundarySucceedsAndOneAthleteOverDoesNotCallTheProvider() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		TeamId teamId = team(ownerId, organizationId);
		seedAthletes(teamId, 25);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_25"));
	}

	@Test
	void preflightDowngradeDoesNotCallTheProvider() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		seedAthletes(team(ownerId, organizationId), 26);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		int calls = ManagementProvider.planChangeCalls;

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_PLAN_CAPACITY_CONFLICT"));
		assertThat(ManagementProvider.planChangeCalls).isEqualTo(calls);
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(OrganizationId.of(organizationId))).isEqualTo(26);
	}

	@Test
	void concurrentAcceptAfterProviderDowngradeRestoresThePreviousPrice() throws Exception {
		VerifiedAccount owner = accounts.registerVerified("slice-e-owner");
		UUID ownerId = owner.accountId().value();
		UUID organizationId = organization(ownerId);
		TeamId teamId = team(ownerId, organizationId);
		seedAthletes(teamId, 25);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		VerifiedAccount athlete = accounts.registerVerifiedAthlete("slice-e-downgrade");
		MvcResult invite = mockMvc.perform(post("/api/v1/teams/" + teamId.value() + "/invitations")
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{ "email": "%s", "role": "ATHLETE" }
								""".formatted(athlete.email())))
				.andExpect(status().isCreated())
				.andReturn();
		String rawToken = JsonPath.read(invite.getResponse().getContentAsString(), "$.rawToken");
		ManagementProvider.mutationStarted = new CountDownLatch(1);
		ManagementProvider.releaseMutation = new CountDownLatch(1);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<MvcResult> change = executor.submit(() -> mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
							.with(authentication(authFor(ownerId)))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content(planBody(UUID.randomUUID(), "ORG_BAND_25", "MONTHLY")))
					.andReturn());
			assertThat(ManagementProvider.mutationStarted.await(15, TimeUnit.SECONDS)).isTrue();
			mockMvc.perform(post("/api/v1/invitations/" + rawToken + "/accept")
							.with(authentication(authFor(athlete.accountId().value())))
							.with(csrf()))
					.andExpect(status().isOk());
			ManagementProvider.releaseMutation.countDown();
			assertThat(change.get(20, TimeUnit.SECONDS).getResponse().getStatus()).isEqualTo(409);
			assertThat(change.get().getResponse().getContentAsString()).contains("ORGANIZATION_PLAN_CAPACITY_CONFLICT");
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(ManagementProvider.restoreCalls).isEqualTo(1);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(OrganizationId.of(organizationId))).isEqualTo(26);
	}

	@Test
	void failedDowngradeRestoreLeavesThePreviousPlanAndWebhookRetryRestoresIt() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		TeamId teamId = team(ownerId, organizationId);
		seedAthletes(teamId, 25);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		ManagementProvider.restoreUnavailable = true;
		ManagementProvider.mutationStarted = new CountDownLatch(1);
		ManagementProvider.releaseMutation = new CountDownLatch(1);

		ExecutorService executor = Executors.newSingleThreadExecutor();
		try {
			Future<MvcResult> change = executor.submit(() -> mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
							.with(authentication(authFor(ownerId)))
							.with(csrf())
							.contentType(MediaType.APPLICATION_JSON)
							.content(planBody(UUID.randomUUID(), "ORG_BAND_25", "MONTHLY")))
					.andReturn());
			assertThat(ManagementProvider.mutationStarted.await(15, TimeUnit.SECONDS)).isTrue();
			seedAthletes(teamId, 1);
			ManagementProvider.releaseMutation.countDown();
			MvcResult result = change.get(20, TimeUnit.SECONDS);
			assertThat(result.getResponse().getStatus()).isEqualTo(502);
			assertThat(result.getResponse().getContentAsString()).contains("BILLING_PROVIDER_UNAVAILABLE");
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		ManagementProvider.restoreUnavailable = false;
		ManagementProvider.webhookCustomerRef = reloaded(subscription).providerCustomerRef();
		ManagementProvider.webhookStatus = ProviderCommercialStatus.ACTIVE;
		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhookPayload(organizationId, subscription.id().value(), "evt_restore_retry")))
				.andExpect(status().isOk());
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(ManagementProvider.restoreCalls).isGreaterThanOrEqualTo(2);
	}

	@Test
	void cadenceChangeKeepsTheSubscriptionAndTrialEnd() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = trialingSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		ManagementProvider.preservedTrialEnd = subscription.trialEndsAt();

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "ANNUAL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_75"))
				.andExpect(jsonPath("$.cadence").value("ANNUAL"))
				.andExpect(jsonPath("$.trialEndsAt").value(ManagementProvider.preservedTrialEnd.toString()));
		assertThat(reloaded(subscription).id()).isEqualTo(subscription.id());
	}

	@Test
	void cancelAndReactivateStayOnTheSameSubscription() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL);
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(requestId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CANCEL_AT_PERIOD_END"));
		assertThat(ManagementProvider.cancelCalls).isEqualTo(1);
		assertThat(auditTypes(organizationId)).filteredOn("BILLING_CANCEL_REQUESTED"::equals).hasSize(1);

		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(requestId)))
				.andExpect(status().isOk());
		assertThat(ManagementProvider.cancelCalls).isEqualTo(1);

		UUID reactivateId = UUID.randomUUID();
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(reactivateId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));
		assertThat(ManagementProvider.reactivateCalls).isEqualTo(1);
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(reactivateId)))
				.andExpect(status().isOk());
		assertThat(ManagementProvider.reactivateCalls).isEqualTo(1);
		assertThat(auditTypes(organizationId)).filteredOn("BILLING_SUBSCRIPTION_REACTIVATED"::equals).hasSize(1);
		assertThat(subscriptionRepository.findBySubject(
				com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION, organizationId)).hasSize(1);
	}

	@Test
	void pastDueAndGraceRejectCancelAndPlanChangeWithoutAProviderCall() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription pastDue = lifecycleSubscription(
				organizationId, CommercialPlanKey.ORG_BAND_75, SubscriptionLifecycleState.PAST_DUE);
		assertDeniedWithoutProvider(ownerId, organizationId, pastDue, "BILLING_LIFECYCLE_CONFLICT");
	}

	@Test
	void gracePeriodRejectsPlanChangeAndCancelWithoutAProviderCall() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription grace = lifecycleSubscription(
				organizationId, CommercialPlanKey.ORG_BAND_75, SubscriptionLifecycleState.GRACE_PERIOD);
		assertDeniedWithoutProvider(ownerId, organizationId, grace, "BILLING_LIFECYCLE_CONFLICT");
		assertThat(reloaded(grace).lifecycleState()).isEqualTo(SubscriptionLifecycleState.GRACE_PERIOD);
	}

	@Test
	void foreignPendingAndExpiredSubscriptionsDoNotMutateTheProvider() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription active = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		int calls = ManagementProvider.planChangeCalls;

		mockMvc.perform(post(planPath(organizationId, UUID.randomUUID()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(planPath(organizationId, active.id().value()))
						.with(authentication(authFor(UUID.randomUUID())))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isNotFound());
		mockMvc.perform(post(planPath(organizationId, active.id().value()))
						.with(authentication(authFor(UUID.randomUUID())))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "INDIVIDUAL_PREMIUM", "MONTHLY")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		Subscription expired = expire(active);
		mockMvc.perform(post(planPath(organizationId, expired.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));

		UUID checkoutId = UUID.randomUUID();
		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(checkoutId, "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isCreated());
		mockMvc.perform(post(planPath(organizationId, checkoutId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_CHECKOUT_IN_PROGRESS"));
		assertThat(ManagementProvider.planChangeCalls).isEqualTo(calls);
	}

	@Test
	void sameRequestWithADifferentTargetConflictsAndUnknownWebhookPriceDoesNotChangePlan() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		UUID requestId = UUID.randomUUID();
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(requestId, "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isOk());
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(requestId, "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_REQUEST_CONFLICT"));
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);

		ManagementProvider.rejectWebhookPrice = true;
		ManagementProvider.webhookCustomerRef = subscription.providerCustomerRef();
		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhookPayload(organizationId, subscription.id().value(), "evt_unknown_price")))
				.andExpect(status().isOk());
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(reloaded(subscription).billingCadence()).isEqualTo(BillingCadence.MONTHLY);
	}

	@Test
	void externalUndersizedPriceIsRestoredAndCheckoutRaceIsNot() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		seedAthletes(team(ownerId, organizationId), 26);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		ManagementProvider.webhookCustomerRef = subscription.providerCustomerRef();
		ManagementProvider.webhookStatus = ProviderCommercialStatus.ACTIVE;

		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
						.contentType(MediaType.APPLICATION_JSON)
						.content(webhookPayload(organizationId, subscription.id().value(), "evt_external_downgrade")))
				.andExpect(status().isOk());

		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
		assertThat(ManagementProvider.restoreCalls).isEqualTo(1);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(OrganizationId.of(organizationId))).isEqualTo(26);
	}

	@Test
	void trialingCancelAndReactivatePreserveTheTrial() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = trialingSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		ManagementProvider.echoedPlan = CommercialPlanKey.ORG_BAND_25;
		ManagementProvider.echoedCadence = BillingCadence.MONTHLY;
		ManagementProvider.preservedTrialEnd = subscription.trialEndsAt();
		ManagementProvider.reactivateStatus = ProviderCommercialStatus.TRIALING;

		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CANCEL_AT_PERIOD_END"))
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_25"));
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("TRIALING"))
				.andExpect(jsonPath("$.trialEndsAt").value(subscription.trialEndsAt().toString()));
		assertThat(reloaded(subscription).trialEndsAt()).isEqualTo(subscription.trialEndsAt());
		assertThat(subscriptionRepository.findBySubject(
				com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION, organizationId)).hasSize(1);
	}

	private Subscription expire(Subscription subscription) {
		subscription.synchronizeProviderSnapshot(new ProviderSubscriptionSnapshot(
				subscription.providerCustomerRef(),
				subscription.providerSubscriptionRef(),
				ProviderCommercialStatus.ENDED,
				false,
				null,
				subscription.currentPeriodEndsAt(),
				subscription.planKey(),
				subscription.billingCadence(),
				Instant.now(clock).plusSeconds(20)), clock);
		return subscriptionRepository.save(subscription);
	}

	@Test
	void ambiguousSubscriptionsBlockManagementAndCurrentReadButNotCapacityCount() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		seedAthletes(team(ownerId, organizationId), 3);
		Subscription first = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);
		activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL);
		int calls = ManagementProvider.totalMutations();

		mockMvc.perform(get("/api/v1/billing/organizations/" + organizationId)
						.with(authentication(authFor(ownerId))))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_SUBSCRIPTION_STATE_CONFLICT"));
		mockMvc.perform(post(portalPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf()))
				.andExpect(status().isConflict());
		mockMvc.perform(post(planPath(organizationId, first.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isConflict());
		mockMvc.perform(post(cancelPath(organizationId, first.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict());
		mockMvc.perform(post(reactivatePath(organizationId, first.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict());
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
		mockMvc.perform(get("/api/v1/billing/organizations/" + organizationId + "/capacity")
						.with(authentication(authFor(ownerId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activeAthleteCount").value(3))
				.andExpect(jsonPath("$.bandCapacity").value(nullValue()));
	}

	private void assertDeniedWithoutProvider(UUID ownerId, UUID organizationId, Subscription subscription, String code)
			throws Exception {
		int calls = ManagementProvider.totalMutations();
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(code));
		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(code));
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value(code));
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
	}

	@Test
	void planCancelAndReactivateAreOwnerOnly() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);
		UUID adminId = membership(organizationId, OrganizationMembershipRole.ORG_ADMIN);
		TeamId teamId = team(ownerId, organizationId);
		UUID coachId = teamRole(teamId, OrganizationMembershipRole.COACH, null);
		UUID headCoachId = teamRole(teamId, OrganizationMembershipRole.HEAD_COACH, null);
		UUID teamAdminId = teamRole(teamId, OrganizationMembershipRole.TEAM_ADMIN, null);
		UUID athleteId = teamRole(teamId, OrganizationMembershipRole.ATHLETE, UUID.randomUUID());
		int calls = ManagementProvider.totalMutations();

		for (UUID actor : List.of(adminId, coachId, headCoachId, teamAdminId, athleteId, UUID.randomUUID())) {
			assertManagementDenied(organizationId, subscription.id().value(), actor);
		}
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
	}

	@Test
	void ownerCanUpgradeFromTeamToOrganizationOnTheSameSubscription() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscription.id().value().toString()))
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_250"));
		assertThat(subscriptionRepository.findBySubject(
				com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION, organizationId)).hasSize(1);
		assertThat(ManagementProvider.planChangeCalls).isEqualTo(1);
		assertThat(auditTypes(organizationId)).filteredOn("BILLING_PLAN_CHANGED"::equals).hasSize(1);
		assertThat(reloaded(subscription).providerSubscriptionRef()).isEqualTo(subscription.providerSubscriptionRef());
	}

	@Test
	void cadenceOnlyChangeIsAllowedWhenTheBandIsAlreadyOverCapacity() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		seedAthletes(team(ownerId, organizationId), 26);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_25, BillingCadence.MONTHLY);

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_25", "ANNUAL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_25"))
				.andExpect(jsonPath("$.cadence").value("ANNUAL"));
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_25);
		assertThat(teamMembershipRepository.countDistinctActiveAthletes(OrganizationId.of(organizationId))).isEqualTo(26);
	}

	@Test
	void sameBandCadenceChangesStayOnTheSubscription() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = activeSubscription(organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY);

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "ANNUAL")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_75"))
				.andExpect(jsonPath("$.cadence").value("ANNUAL"))
				.andExpect(jsonPath("$.subscriptionId").value(subscription.id().value().toString()));
		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_75"))
				.andExpect(jsonPath("$.cadence").value("MONTHLY"));
		assertThat(subscriptionRepository.findBySubject(
				com.devinolabs.uap.entitlements.BillingSubjectType.ORGANIZATION, organizationId)).hasSize(1);
	}

	@Test
	void scheduledCancellationBlocksPlanChangeAndRepeatCancelDoesNotCallTheProvider() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = scheduledCancellation(
				organizationId,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.MONTHLY,
				Instant.now(clock).plusSeconds(30 * 24 * 60 * 60));
		int calls = ManagementProvider.totalMutations();

		mockMvc.perform(post(planPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_LIFECYCLE_CONFLICT"));
		mockMvc.perform(post(cancelPath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CANCEL_AT_PERIOD_END"));
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
		assertThat(reloaded(subscription).planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
	}

	@Test
	void reactivateAfterThePaidThroughInstantIsRejected() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription subscription = scheduledCancellation(
				organizationId,
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL,
				Instant.now(clock).minusSeconds(5));
		int calls = ManagementProvider.totalMutations();

		mockMvc.perform(post(reactivatePath(organizationId, subscription.id().value()))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
	}

	@Test
	void pendingAndExpiredCancelAndReactivateAreNotManageable() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = organization(ownerId);
		Subscription expired = expire(activeSubscription(
				organizationId, CommercialPlanKey.ORG_BAND_75, BillingCadence.MONTHLY));
		int calls = ManagementProvider.totalMutations();
		assertNotManageable(ownerId, organizationId, expired.id().value());

		UUID checkoutId = UUID.randomUUID();
		mockMvc.perform(post(checkoutPath(organizationId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(checkoutBody(checkoutId, "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isCreated());
		assertNotManageable(ownerId, organizationId, checkoutId);
		assertThat(ManagementProvider.cancelCalls + ManagementProvider.reactivateCalls).isZero();
		assertThat(ManagementProvider.totalMutations()).isEqualTo(calls);
	}

	private void assertManagementDenied(UUID organizationId, UUID subscriptionId, UUID actor) throws Exception {
		mockMvc.perform(post(planPath(organizationId, subscriptionId))
						.with(authentication(authFor(actor)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(planBody(UUID.randomUUID(), "ORG_BAND_250", "MONTHLY")))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
		mockMvc.perform(post(cancelPath(organizationId, subscriptionId))
						.with(authentication(authFor(actor)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
		mockMvc.perform(post(reactivatePath(organizationId, subscriptionId))
						.with(authentication(authFor(actor)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));
	}

	private void assertNotManageable(UUID ownerId, UUID organizationId, UUID subscriptionId) throws Exception {
		mockMvc.perform(post(cancelPath(organizationId, subscriptionId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
		mockMvc.perform(post(reactivatePath(organizationId, subscriptionId))
						.with(authentication(authFor(ownerId)))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content(requestBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_SUBSCRIPTION_NOT_MANAGEABLE"));
	}

	private List<String> auditTypes(UUID organizationId) {
		return auditRepository.findLatestByOrganizationId(organizationId, 30).stream()
				.map(SecurityAuditRecord::eventType)
				.toList();
	}

	private Subscription reloaded(Subscription subscription) {
		return subscriptionRepository.findById(subscription.id()).orElseThrow();
	}

	private UUID organization(UUID ownerId) {
		return createOrganizationUseCase.execute(AccountId.of(ownerId), "Slice E " + ownerId).id().value();
	}

	private TeamId team(UUID ownerId, UUID organizationId) {
		Team team = teamRepository.save(Team.register(
				TeamId.generate(),
				OrganizationId.of(organizationId),
				"Slice E Team",
				clock));
		return team.id();
	}

	private UUID membership(UUID organizationId, OrganizationMembershipRole role) {
		UUID accountId = UUID.randomUUID();
		membershipRepository.save(OrganizationMembership.register(
				OrganizationMembershipId.generate(),
				OrganizationId.of(organizationId),
				AccountId.of(accountId),
				null,
				role,
				clock));
		return accountId;
	}

	private UUID teamRole(TeamId teamId, OrganizationMembershipRole role, UUID athleteId) {
		UUID accountId = UUID.randomUUID();
		teamMembershipRepository.save(TeamMembership.register(
				TeamMembershipId.generate(),
				teamId,
				AccountId.of(accountId),
				athleteId,
				role,
				clock));
		return accountId;
	}

	private void seedAthletes(TeamId teamId, int count) {
		for (int index = 0; index < count; index++) {
			teamMembershipRepository.save(TeamMembership.register(
					TeamMembershipId.generate(),
					teamId,
					AccountId.of(UUID.randomUUID()),
					UUID.randomUUID(),
					OrganizationMembershipRole.ATHLETE,
					clock));
		}
	}

	private Subscription scheduledCancellation(
			UUID organizationId,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			Instant periodEndsAt) {
		Subscription subscription = activeSubscription(organizationId, planKey, cadence);
		subscription.synchronizeProviderSnapshot(new ProviderSubscriptionSnapshot(
				subscription.providerCustomerRef(),
				subscription.providerSubscriptionRef(),
				ProviderCommercialStatus.ACTIVE,
				true,
				null,
				periodEndsAt,
				planKey,
				cadence,
				Instant.now(clock).plusSeconds(30)), clock);
		return subscriptionRepository.save(subscription);
	}

	private Subscription activeSubscription(
			UUID organizationId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		return saveSnapshot(organizationId, planKey, cadence, ProviderCommercialStatus.ACTIVE, false, null);
	}

	private Subscription trialingSubscription(
			UUID organizationId,
			CommercialPlanKey planKey,
			BillingCadence cadence) {
		return saveSnapshot(
				organizationId,
				planKey,
				cadence,
				ProviderCommercialStatus.TRIALING,
				false,
				Instant.now(clock).plusSeconds(10 * 24 * 60 * 60));
	}

	private Subscription lifecycleSubscription(
			UUID organizationId,
			CommercialPlanKey planKey,
			SubscriptionLifecycleState state) {
		Subscription subscription = activeSubscription(organizationId, planKey, BillingCadence.MONTHLY);
		if (state == SubscriptionLifecycleState.PAST_DUE) {
			subscription.markPastDue(clock);
		}
		else if (state == SubscriptionLifecycleState.GRACE_PERIOD) {
			subscription.enterGracePeriod(clock);
		}
		return subscriptionRepository.save(subscription);
	}

	private Subscription saveSnapshot(
			UUID organizationId,
			CommercialPlanKey planKey,
			BillingCadence cadence,
			ProviderCommercialStatus status,
			boolean cancelAtPeriodEnd,
			Instant trialEndsAt) {
		UUID subscriptionId = UUID.randomUUID();
		Subscription subscription = Subscription.startPendingOrganizationCheckout(
				SubscriptionId.of(subscriptionId),
				BillingSubject.organization(organizationId),
				planKey,
				cadence,
				clock);
		subscription.synchronizeProviderSnapshot(new ProviderSubscriptionSnapshot(
				"cus_from_sub_" + subscriptionId,
				"sub_" + subscriptionId,
				status,
				cancelAtPeriodEnd,
				trialEndsAt,
				Instant.now(clock).plusSeconds(30 * 24 * 60 * 60),
				planKey,
				cadence,
				Instant.now(clock).plusSeconds(1)), clock);
		return subscriptionRepository.save(subscription);
	}

	private static String portalPath(UUID organizationId) {
		return "/api/v1/billing/organizations/" + organizationId + "/portal-sessions";
	}

	private static String checkoutPath(UUID organizationId) {
		return "/api/v1/billing/organizations/" + organizationId + "/checkout-sessions";
	}

	private static String planPath(UUID organizationId, UUID subscriptionId) {
		return "/api/v1/billing/organizations/" + organizationId + "/subscriptions/" + subscriptionId + "/plan-changes";
	}

	private static String cancelPath(UUID organizationId, UUID subscriptionId) {
		return "/api/v1/billing/organizations/" + organizationId + "/subscriptions/" + subscriptionId + "/cancel";
	}

	private static String reactivatePath(UUID organizationId, UUID subscriptionId) {
		return "/api/v1/billing/organizations/" + organizationId + "/subscriptions/" + subscriptionId + "/reactivate";
	}

	private static String checkoutBody(UUID requestId, String planKey, String cadence) {
		return "{\"requestId\":\"" + requestId + "\",\"planKey\":\"" + planKey + "\",\"cadence\":\"" + cadence + "\"}";
	}

	private static String planBody(UUID requestId, String planKey, String cadence) {
		return "{\"requestId\":\"" + requestId + "\",\"targetPlanKey\":\"" + planKey
				+ "\",\"targetCadence\":\"" + cadence + "\"}";
	}

	private static String requestBody(UUID requestId) {
		return "{\"requestId\":\"" + requestId + "\"}";
	}

	private static String webhookPayload(UUID organizationId, UUID subscriptionId, String eventId) {
		return "{\"eventId\":\"" + eventId + "\",\"organizationId\":\"" + organizationId
				+ "\",\"subscriptionId\":\"" + subscriptionId + "\"}";
	}

	private static UsernamePasswordAuthenticationToken authFor(UUID accountId) {
		AccountPrincipal principal = new AccountPrincipal(com.devinolabs.uap.identity.domain.AccountId.of(accountId));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
	}

	@TestConfiguration
	static class ProviderConfig {

		@Bean
		@Primary
		OrganizationBillingProvider managementBillingProvider() {
			return new ManagementProvider();
		}
	}

	static final class ManagementProvider implements OrganizationBillingProvider {

		static int portalCalls;
		static int checkoutCalls;
		static int customerCalls;
		static int planChangeCalls;
		static int restoreCalls;
		static int cancelCalls;
		static int reactivateCalls;
		static String portalCustomer;
		static boolean paymentNotApplied;
		static boolean unavailable;
		static boolean restoreUnavailable;
		static boolean rejectWebhookPrice;
		static String webhookCustomerRef;
		static ProviderCommercialStatus webhookStatus;
		static final java.util.Map<UUID, String> planRequestTargets = new java.util.concurrent.ConcurrentHashMap<>();
		static Instant preservedTrialEnd;
		static CommercialPlanKey echoedPlan;
		static BillingCadence echoedCadence;
		static ProviderCommercialStatus reactivateStatus;
		static CountDownLatch mutationStarted;
		static CountDownLatch releaseMutation;

		static void reset() {
			portalCalls = 0;
			checkoutCalls = 0;
			customerCalls = 0;
			planChangeCalls = 0;
			restoreCalls = 0;
			cancelCalls = 0;
			reactivateCalls = 0;
			portalCustomer = null;
			paymentNotApplied = false;
			unavailable = false;
			restoreUnavailable = false;
			preservedTrialEnd = null;
			echoedPlan = null;
			echoedCadence = null;
			reactivateStatus = null;
			rejectWebhookPrice = false;
			webhookCustomerRef = null;
			webhookStatus = ProviderCommercialStatus.TRIALING;
			planRequestTargets.clear();
			mutationStarted = null;
			releaseMutation = null;
		}

		static int totalMutations() {
			return portalCalls + planChangeCalls + restoreCalls + cancelCalls + reactivateCalls;
		}

		@Override
		public String createCustomer(UUID organizationId) {
			customerCalls++;
			return "cus_test_" + organizationId;
		}

		@Override
		public CheckoutSession createCheckoutSession(
				UUID organizationId,
				UUID subscriptionId,
				String providerCustomerRef,
				CommercialPlanKey planKey,
				BillingCadence cadence) {
			checkoutCalls++;
			return new CheckoutSession("cs_test_" + subscriptionId, "https://checkout.stripe.test/" + subscriptionId);
		}

		@Override
		public ProviderSubscriptionSnapshot fetchCheckoutSubscription(
				UUID organizationId,
				UUID subscriptionId,
				String checkoutSessionId,
				String providerCustomerRef,
				CommercialPlanKey planKey,
				BillingCadence cadence) {
			return snapshot(providerCustomerRef, subscriptionId, planKey, cadence, false, ProviderCommercialStatus.TRIALING);
		}

		@Override
		public VerifiedProviderEvent verifyWebhook(byte[] payload, String signatureHeader) {
			String body = new String(payload);
			return new VerifiedProviderEvent(
					stringField(body, "eventId"),
					"checkout.session.completed",
					false,
					Instant.now().plusSeconds(30),
					"cs_test",
					"sub_test_" + uuidField(body, "subscriptionId"),
					uuidField(body, "organizationId"),
					uuidField(body, "subscriptionId"));
		}

		@Override
		public ProviderSubscriptionSnapshot fetchAuthoritativeSnapshot(VerifiedProviderEvent event) {
			if (rejectWebhookPrice) {
				throw new BillingConflictException(
						"BILLING_PROVIDER_PRICE_REJECTED", "Billing provider price could not be accepted");
			}
			String customer = webhookCustomerRef == null
					? "cus_test_" + event.organizationId()
					: webhookCustomerRef;
			ProviderCommercialStatus status = webhookStatus == null
					? ProviderCommercialStatus.TRIALING
					: webhookStatus;
			return snapshot(
					customer,
					event.subscriptionId(),
					CommercialPlanKey.ORG_BAND_25,
					BillingCadence.MONTHLY,
					false,
					status);
		}

		@Override
		public PortalSession createPortalSession(UUID organizationId, String providerCustomerRef) {
			portalCalls++;
			portalCustomer = providerCustomerRef;
			return new PortalSession("https://billing.stripe.test/portal/" + organizationId);
		}

		@Override
		public ProviderSubscriptionSnapshot changeSubscriptionPlan(
				UUID subscriptionId,
				String providerSubscriptionRef,
				CommercialPlanKey targetPlanKey,
				BillingCadence targetCadence,
				UUID requestId) {
			planChangeCalls++;
			String target = targetPlanKey.name() + ":" + targetCadence.name();
			String previous = planRequestTargets.putIfAbsent(requestId, target);
			if (previous != null && !previous.equals(target)) {
				throw new BillingConflictException(
						"BILLING_REQUEST_CONFLICT", "Billing request was already used");
			}
			if (paymentNotApplied) {
				throw new BillingConflictException("BILLING_PAYMENT_NOT_APPLIED", "The billing change was not applied");
			}
			if (unavailable) {
				throw new BillingProviderUnavailableException(new IllegalStateException("down"));
			}
			awaitMutationGate();
			ProviderCommercialStatus status = preservedTrialEnd == null
					? ProviderCommercialStatus.ACTIVE
					: ProviderCommercialStatus.TRIALING;
			ProviderSubscriptionSnapshot snapshot = snapshot(
					customerFrom(providerSubscriptionRef), subscriptionId, targetPlanKey, targetCadence, false, status);
			if (preservedTrialEnd != null) {
				return new ProviderSubscriptionSnapshot(
						snapshot.providerCustomerRef(),
						snapshot.providerSubscriptionRef(),
						ProviderCommercialStatus.TRIALING,
						false,
						preservedTrialEnd,
						snapshot.currentPeriodEndsAt(),
						targetPlanKey,
						targetCadence,
						snapshot.providerStateAsOf());
			}
			return snapshot;
		}

		@Override
		public ProviderSubscriptionSnapshot restoreSubscriptionPlan(
				UUID subscriptionId,
				String providerSubscriptionRef,
				CommercialPlanKey planKey,
				BillingCadence cadence,
				String operationToken) {
			restoreCalls++;
			if (restoreUnavailable) {
				throw new BillingProviderUnavailableException(new IllegalStateException("restore down"));
			}
			return snapshot(customerFrom(providerSubscriptionRef), subscriptionId, planKey, cadence, false,
					ProviderCommercialStatus.ACTIVE);
		}

		@Override
		public ProviderSubscriptionSnapshot scheduleCancelAtPeriodEnd(
				UUID subscriptionId,
				String providerSubscriptionRef,
				UUID requestId) {
			cancelCalls++;
			CommercialPlanKey plan = echoedPlan == null ? CommercialPlanKey.ORG_BAND_75 : echoedPlan;
			BillingCadence cadence = echoedCadence == null ? BillingCadence.ANNUAL : echoedCadence;
			return snapshot(
					customerFrom(providerSubscriptionRef),
					subscriptionId,
					plan,
					cadence,
					true,
					ProviderCommercialStatus.ACTIVE);
		}

		@Override
		public ProviderSubscriptionSnapshot reactivateSubscription(
				UUID subscriptionId,
				String providerSubscriptionRef,
				UUID requestId) {
			reactivateCalls++;
			CommercialPlanKey plan = echoedPlan == null ? CommercialPlanKey.ORG_BAND_75 : echoedPlan;
			BillingCadence cadence = echoedCadence == null ? BillingCadence.ANNUAL : echoedCadence;
			ProviderCommercialStatus status = reactivateStatus == null
					? ProviderCommercialStatus.ACTIVE
					: reactivateStatus;
			ProviderSubscriptionSnapshot restored = snapshot(
					customerFrom(providerSubscriptionRef),
					subscriptionId,
					plan,
					cadence,
					false,
					status);
			if (status != ProviderCommercialStatus.TRIALING || preservedTrialEnd == null) {
				return restored;
			}
			return new ProviderSubscriptionSnapshot(
					restored.providerCustomerRef(),
					restored.providerSubscriptionRef(),
					status,
					false,
					preservedTrialEnd,
					restored.currentPeriodEndsAt(),
					plan,
					cadence,
					restored.providerStateAsOf());
		}

		@Override
		public ProviderSubscriptionSnapshot fetchSubscription(String providerSubscriptionRef) {
			throw new UnsupportedOperationException("fetchSubscription");
		}

		private static void awaitMutationGate() {
			if (mutationStarted == null || releaseMutation == null) {
				return;
			}
			mutationStarted.countDown();
			try {
				if (!releaseMutation.await(15, TimeUnit.SECONDS)) {
					throw new IllegalStateException("Downgrade mutation was not released");
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException("Downgrade mutation interrupted", ex);
			}
		}

		private static ProviderSubscriptionSnapshot snapshot(
				String customerRef,
				UUID subscriptionId,
				CommercialPlanKey planKey,
				BillingCadence cadence,
				boolean cancelAtPeriodEnd,
				ProviderCommercialStatus status) {
			Instant trialEnd = status == ProviderCommercialStatus.TRIALING
					? Instant.now().plusSeconds(10 * 24 * 60 * 60)
					: null;
			return new ProviderSubscriptionSnapshot(
					customerRef,
					"sub_" + subscriptionId,
					status,
					cancelAtPeriodEnd,
					trialEnd,
					Instant.now().plusSeconds(30 * 24 * 60 * 60),
					planKey,
					cadence,
					Instant.now().plusSeconds(5));
		}

		private static String customerFrom(String providerSubscriptionRef) {
			return "cus_from_" + providerSubscriptionRef;
		}

		private static UUID uuidField(String body, String name) {
			return UUID.fromString(stringField(body, name));
		}

		private static String stringField(String body, String name) {
			String needle = "\"" + name + "\":\"";
			int start = body.indexOf(needle) + needle.length();
			return body.substring(start, body.indexOf('"', start));
		}
	}

}
