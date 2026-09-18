package com.devinolabs.uap.billing.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

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

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.billing.application.InvalidWebhookSignatureException;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider;
import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.SubscriptionRepository;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;
import com.devinolabs.uap.billing.domain.ProviderSubscriptionSnapshot;
import com.devinolabs.uap.billing.domain.SubscriptionId;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.OrganizationMembershipRepository;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembership;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;

@SpringBootTest(properties = {
		"uap.billing.stripe.enabled=true",
		"uap.billing.stripe.secret-key=rk_test_placeholder_for_http_tests",
		"uap.billing.stripe.webhook-secret=whsec_placeholder_for_http_tests",
		"uap.billing.stripe.success-url=https://app.example.com/billing/success?session_id={CHECKOUT_SESSION_ID}",
		"uap.billing.stripe.cancel-url=https://app.example.com/billing/cancel",
		"uap.billing.stripe.prices.org-band-25-monthly=price_http_25_monthly",
		"uap.billing.stripe.prices.org-band-25-annual=price_http_25_annual",
		"uap.billing.stripe.prices.org-band-75-monthly=price_http_75_monthly",
		"uap.billing.stripe.prices.org-band-75-annual=price_http_75_annual",
		"uap.billing.stripe.prices.org-band-250-monthly=price_http_250_monthly",
		"uap.billing.stripe.prices.org-band-250-annual=price_http_250_annual"
})
@AutoConfigureMockMvc
@Import({ TestcontainersConfiguration.class, OrganizationBillingHttpIntegrationTests.ProviderConfig.class })
class OrganizationBillingHttpIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private CreateOrganizationUseCase createOrganizationUseCase;

	@Autowired
	private SubscriptionRepository subscriptionRepository;

	@Autowired
	private OrganizationMembershipRepository membershipRepository;

	@Test
	void billingCheckoutRequiresAuthenticationAndCsrf() throws Exception {
		UUID organizationId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		String body = checkoutBody(requestId, "ORG_BAND_25", "MONTHLY");

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(UUID.randomUUID())))
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isForbidden());
	}

	@Test
	void onlyOrganizationOwnerCanCreateCheckoutWithoutClientPriceOrAmount() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID administratorId = UUID.randomUUID();
		UUID foreignAccountId = UUID.randomUUID();
		UUID organizationId = createOrganizationUseCase.execute(AccountId.of(ownerId), "Billing Test Org")
				.id().value();
		membershipRepository.save(OrganizationMembership.register(
				OrganizationMembershipId.generate(),
				OrganizationId.of(organizationId),
				AccountId.of(administratorId),
				null,
				OrganizationMembershipRole.ORG_ADMIN,
				Clock.systemUTC()));
		UUID requestId = UUID.randomUUID();
		String body = checkoutBody(requestId, "ORG_BAND_75", "ANNUAL");

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(foreignAccountId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(administratorId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ORGANIZATION_NOT_FOUND"));

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subscriptionId").value(requestId.toString()))
				.andExpect(jsonPath("$.checkoutSessionId").value("cs_test_" + requestId))
				.andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.test/" + requestId))
				.andExpect(jsonPath("$.priceId").doesNotExist())
				.andExpect(jsonPath("$.amount").doesNotExist());

		assertThat(subscriptionRepository.findById(SubscriptionId.of(requestId)))
				.get()
				.satisfies(subscription -> {
					assertThat(subscription.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
					assertThat(subscription.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
					assertThat(subscription.lifecycleState()).isEqualTo(SubscriptionLifecycleState.PENDING);
					assertThat(subscription.isCommerciallyEntitledAt(Instant.now())).isFalse();
				});
	}

	@Test
	void syncRefetchesProviderStateAndInvalidPlanFailsValidation() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = createOrganizationUseCase.execute(AccountId.of(ownerId), "Billing Sync Org")
				.id().value();
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "INDIVIDUAL_PREMIUM", "MONTHLY")))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isCreated());

		mockMvc.perform(post("/api/v1/billing/organizations/{organizationId}/subscriptions/{subscriptionId}/sync",
						organizationId, requestId)
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"checkoutSessionId\":\"cs_test_" + requestId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("TRIALING"))
				.andExpect(jsonPath("$.cadence").value("MONTHLY"));

		mockMvc.perform(get("/api/v1/billing/organizations/" + organizationId)
					.with(authentication(authFor(ownerId))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("TRIALING"))
				.andExpect(jsonPath("$.planKey").value("ORG_BAND_25"));
	}

	@Test
	void stripeWebhookVerifiesSignatureIsReplaySafeAndDoesNotRequireCsrf() throws Exception {
		FakeOrganizationBillingProvider.fetchCount = 0;
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = createOrganizationUseCase.execute(AccountId.of(ownerId), "Webhook Org")
				.id().value();
		UUID requestId = UUID.randomUUID();
		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isCreated());

		String payload = webhookPayload(organizationId, requestId, "evt_http_1");
		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Stripe-Signature", "invalid")
					.content(payload))
				.andExpect(status().isBadRequest());

		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Stripe-Signature", "sig_valid")
					.content(payload))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/v1/billing/webhooks/stripe")
					.contentType(MediaType.APPLICATION_JSON)
					.header("Stripe-Signature", "sig_valid")
					.content(payload))
				.andExpect(status().isOk());

		assertThat(subscriptionRepository.findById(SubscriptionId.of(requestId)))
				.get()
				.satisfies(subscription -> assertThat(subscription.lifecycleState())
						.isEqualTo(SubscriptionLifecycleState.TRIALING));
		assertThat(FakeOrganizationBillingProvider.fetchCount).isEqualTo(1);
	}

	@Test
	void checkoutValidationConflictAndOpenRelationshipReturnSafeCodes() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = createOrganizationUseCase.execute(AccountId.of(ownerId), "Billing Conflict Org")
				.id().value();
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"planKey\":\"ORG_BAND_25\",\"cadence\":\"MONTHLY\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.fieldErrors[0].field").value("requestId"));

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "ORG_BAND_25", "MONTHLY")))
				.andExpect(status().isCreated());

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(UUID.randomUUID(), "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_CHECKOUT_IN_PROGRESS"));

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "ORG_BAND_75", "MONTHLY")))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_REQUEST_CONFLICT"));
	}

	@Test
	void providerFailureReturnsSafeErrorAndRollsBackPendingSubscription() throws Exception {
		UUID ownerId = UUID.randomUUID();
		UUID organizationId = createOrganizationUseCase.execute(AccountId.of(ownerId), "Provider Failure Org")
				.id().value();
		UUID requestId = UUID.randomUUID();

		mockMvc.perform(post(checkoutPath(organizationId))
					.with(authentication(authFor(ownerId)))
					.with(csrf())
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId, "ORG_BAND_250", "ANNUAL")))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.code").value("BILLING_PROVIDER_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("Billing provider request failed"));

		assertThat(subscriptionRepository.findById(SubscriptionId.of(requestId))).isEmpty();
	}

	private static String checkoutPath(UUID organizationId) {
		return "/api/v1/billing/organizations/" + organizationId + "/checkout-sessions";
	}

	private static String checkoutBody(UUID requestId, String planKey, String cadence) {
		return "{\"requestId\":\"" + requestId + "\",\"planKey\":\"" + planKey
				+ "\",\"cadence\":\"" + cadence + "\"}";
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
		OrganizationBillingProvider testOrganizationBillingProvider() {
			return new FakeOrganizationBillingProvider();
		}
	}

	static final class FakeOrganizationBillingProvider implements OrganizationBillingProvider {

		static int fetchCount;

		FakeOrganizationBillingProvider() {
			fetchCount = 0;
		}

		@Override
		public String createCustomer(UUID organizationId) {
			return "cus_test_" + organizationId;
		}

		@Override
		public CheckoutSession createCheckoutSession(
				UUID organizationId,
				UUID subscriptionId,
				String providerCustomerRef,
				CommercialPlanKey planKey,
				BillingCadence cadence) {
			if (planKey == CommercialPlanKey.ORG_BAND_250) {
				throw new BillingProviderUnavailableException(
						new IllegalStateException("simulated provider detail must not escape"));
			}
			return new CheckoutSession(
					"cs_test_" + subscriptionId,
					"https://checkout.stripe.test/" + subscriptionId);
		}

		@Override
		public ProviderSubscriptionSnapshot fetchCheckoutSubscription(
				UUID organizationId,
				UUID subscriptionId,
				String checkoutSessionId,
				String providerCustomerRef,
				CommercialPlanKey planKey,
				BillingCadence cadence) {
			return snapshot(providerCustomerRef, subscriptionId, Instant.now().plusSeconds(1));
		}

		@Override
		public VerifiedProviderEvent verifyWebhook(byte[] payload, String signatureHeader) {
			if (!"sig_valid".equals(signatureHeader)) {
				throw new InvalidWebhookSignatureException();
			}
			String body = new String(payload);
			UUID organizationId = uuidField(body, "organizationId");
			UUID subscriptionId = uuidField(body, "subscriptionId");
			String eventId = stringField(body, "eventId");
			return new VerifiedProviderEvent(
					eventId,
					"checkout.session.completed",
					false,
					Instant.now().plusSeconds(5),
					"cs_test_" + subscriptionId,
					"sub_test_" + subscriptionId,
					organizationId,
					subscriptionId);
		}

		@Override
		public ProviderSubscriptionSnapshot fetchAuthoritativeSnapshot(VerifiedProviderEvent event) {
			fetchCount++;
			return snapshot("cus_test_" + event.organizationId(), event.subscriptionId(), event.createdAt());
		}

		private static ProviderSubscriptionSnapshot snapshot(
				String customerRef,
				UUID subscriptionId,
				Instant providerAsOf) {
			return new ProviderSubscriptionSnapshot(
					customerRef,
					"sub_test_" + subscriptionId,
					ProviderCommercialStatus.TRIALING,
					false,
					Instant.now().plusSeconds(14 * 24 * 60 * 60),
					Instant.now().plusSeconds(30 * 24 * 60 * 60),
					providerAsOf);
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
