package com.devinolabs.uap.billing.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
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

import com.devinolabs.uap.billing.application.BillingAccountNotFoundException;
import com.devinolabs.uap.billing.application.BillingConflictException;
import com.devinolabs.uap.billing.application.IndividualCheckoutService;
import com.devinolabs.uap.billing.application.IndividualSubscriptionManagementService;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class IndividualBillingControllerTests {

	@Mock
	private IndividualCheckoutService checkoutService;

	@Mock
	private IndividualSubscriptionManagementService managementService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(
						new IndividualBillingController(checkoutService, managementService))
				.setControllerAdvice(new BillingExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
				.build();
	}

	@Test
	void currentRequiresAuthenticatedAccountPrincipal() throws Exception {
		assertThatThrownBy(() -> mockMvc.perform(get("/api/v1/billing/account")).andReturn())
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.cause()
				.hasMessageContaining("Authenticated AccountPrincipal is required");

		verify(checkoutService, never()).currentStatus(any());
	}

	@Test
	void currentReturnsSubscriptionForAuthenticatedAccount() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(checkoutService.currentStatus(accountId)).thenReturn(subscriptionStatus(subscriptionId));

		mockMvc.perform(get("/api/v1/billing/account").principal(authFor(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
				.andExpect(jsonPath("$.planKey").value("INDIVIDUAL_PREMIUM"))
				.andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));
	}

	@Test
	void currentMapsAccountNotFoundTo404() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(checkoutService.currentStatus(accountId)).thenThrow(new BillingAccountNotFoundException());

		mockMvc.perform(get("/api/v1/billing/account").principal(authFor(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void createCheckoutHappyPath() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(checkoutService.startCheckout(
				accountId,
				requestId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY))
				.thenReturn(new IndividualCheckoutService.CheckoutResult(
						requestId, "cs_http", "https://checkout.stripe.test/cs_http"));

		mockMvc.perform(post("/api/v1/billing/account/checkout-sessions")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.subscriptionId").value(requestId.toString()))
				.andExpect(jsonPath("$.checkoutSessionId").value("cs_http"))
				.andExpect(jsonPath("$.checkoutUrl").value("https://checkout.stripe.test/cs_http"));
	}

	@Test
	void createCheckoutMapsConflict() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(checkoutService.startCheckout(any(), any(), any(), any()))
				.thenThrow(new BillingConflictException(
						"BILLING_CHECKOUT_IN_PROGRESS",
						"Account already has an open Individual checkout"));

		mockMvc.perform(post("/api/v1/billing/account/checkout-sessions")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(checkoutBody(requestId)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_CHECKOUT_IN_PROGRESS"));
	}

	@Test
	void synchronizeHappyPathAndNotFound() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(checkoutService.synchronize(accountId, subscriptionId, "cs_sync"))
				.thenReturn(subscriptionStatus(subscriptionId));

		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/sync")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"checkoutSessionId\":\"cs_sync\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()));

		when(checkoutService.synchronize(eq(accountId), eq(subscriptionId), eq("cs_missing")))
				.thenThrow(new BillingAccountNotFoundException());

		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/sync")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"checkoutSessionId\":\"cs_missing\"}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void openPortalHappyPathAndNotFound() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(managementService.openPortal(accountId))
				.thenReturn(new IndividualSubscriptionManagementService.PortalSessionResult(
						"https://billing.stripe.test/account-portal"));

		mockMvc.perform(post("/api/v1/billing/account/portal-sessions").principal(authFor(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://billing.stripe.test/account-portal"));

		when(managementService.openPortal(accountId)).thenThrow(new BillingAccountNotFoundException());
		mockMvc.perform(post("/api/v1/billing/account/portal-sessions").principal(authFor(accountId)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void cancelAndReactivateHappyPathAndForeignNotFound() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		UUID requestId = UUID.randomUUID();
		when(managementService.cancel(accountId, subscriptionId, requestId))
				.thenReturn(subscriptionStatus(
						subscriptionId, SubscriptionLifecycleState.CANCEL_AT_PERIOD_END));
		when(managementService.reactivate(accountId, subscriptionId, requestId))
				.thenReturn(subscriptionStatus(subscriptionId));

		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/cancel")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(mutationBody(requestId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("CANCEL_AT_PERIOD_END"));

		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/reactivate")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(mutationBody(requestId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));

		when(managementService.cancel(eq(accountId), eq(subscriptionId), any()))
				.thenThrow(new BillingAccountNotFoundException());
		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/cancel")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(mutationBody(UUID.randomUUID())))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
	}

	@Test
	void cancelMapsLifecycleConflict() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(managementService.cancel(any(), any(), any()))
				.thenThrow(new BillingConflictException(
						"BILLING_LIFECYCLE_CONFLICT",
						"This subscription cannot be changed in its current state"));

		mockMvc.perform(post("/api/v1/billing/account/subscriptions/" + subscriptionId + "/cancel")
					.principal(authFor(accountId))
					.contentType(MediaType.APPLICATION_JSON)
					.content(mutationBody(UUID.randomUUID())))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("BILLING_LIFECYCLE_CONFLICT"));
	}

	private static IndividualCheckoutService.SubscriptionResult subscriptionStatus(UUID subscriptionId) {
		return subscriptionStatus(subscriptionId, SubscriptionLifecycleState.ACTIVE);
	}

	private static IndividualCheckoutService.SubscriptionResult subscriptionStatus(
			UUID subscriptionId,
			SubscriptionLifecycleState lifecycleState) {
		return new IndividualCheckoutService.SubscriptionResult(
				subscriptionId,
				CommercialPlanKey.INDIVIDUAL_PREMIUM,
				BillingCadence.MONTHLY,
				lifecycleState,
				null,
				Instant.parse("2026-10-01T00:00:00Z"),
				null);
	}

	private static String checkoutBody(UUID requestId) {
		return "{\"requestId\":\"" + requestId
				+ "\",\"planKey\":\"INDIVIDUAL_PREMIUM\",\"cadence\":\"MONTHLY\"}";
	}

	private static String mutationBody(UUID requestId) {
		return "{\"requestId\":\"" + requestId + "\"}";
	}

	private static UsernamePasswordAuthenticationToken authFor(UUID accountId) {
		AccountPrincipal principal = new AccountPrincipal(
				com.devinolabs.uap.identity.domain.AccountId.of(accountId));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
	}

}
