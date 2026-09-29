package com.devinolabs.uap.billing.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
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

import com.devinolabs.uap.billing.application.ApplePurchaseValidationService;
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.IndividualManagementChannel;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class AppleBillingControllerTests {

	@Mock
	private ApplePurchaseValidationService purchaseValidationService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new AppleBillingController(purchaseValidationService))
				.setControllerAdvice(new BillingExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
				.build();
	}

	@Test
	void validateRequiresAuthenticatedAccountPrincipal() throws Exception {
		assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/billing/account/apple/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"signedTransactionInfo\":\"header.payload.sig\"}"))
				.andReturn())
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.cause()
				.hasMessageContaining("Authenticated AccountPrincipal is required");

		verify(purchaseValidationService, never()).validateOrRestore(any(), any());
	}

	@Test
	void validateRejectsBlankSignedTransaction() throws Exception {
		mockMvc.perform(post("/api/v1/billing/account/apple/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"signedTransactionInfo\":\"   \"}")
						.principal(auth(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verify(purchaseValidationService, never()).validateOrRestore(any(), any());
	}

	@Test
	void validateReturnsSubscriptionForAuthenticatedAccount() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(purchaseValidationService.validateOrRestore(eq(accountId), eq("header.payload.sig")))
				.thenReturn(new ApplePurchaseValidationService.SubscriptionResult(
						subscriptionId,
						BillingProvider.APPLE_APP_STORE,
						IndividualManagementChannel.APPLE_APP_STORE,
						CommercialPlanKey.INDIVIDUAL_PREMIUM,
						BillingCadence.MONTHLY,
						SubscriptionLifecycleState.ACTIVE,
						null,
						Instant.parse("2026-10-01T00:00:00Z"),
						null));

		mockMvc.perform(post("/api/v1/billing/account/apple/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"signedTransactionInfo\":\"header.payload.sig\"}")
						.principal(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
				.andExpect(jsonPath("$.provider").value("APPLE_APP_STORE"))
				.andExpect(jsonPath("$.managementChannel").value("APPLE_APP_STORE"))
				.andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));
	}

	@Test
	void invalidPurchaseMapsToBadRequest() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(purchaseValidationService.validateOrRestore(eq(accountId), any()))
				.thenThrow(new InvalidApplePurchaseException("rejected"));

		mockMvc.perform(post("/api/v1/billing/account/apple/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"signedTransactionInfo\":\"header.payload.sig\"}")
						.principal(auth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BILLING_APPLE_PURCHASE_REJECTED"));
	}

	private static UsernamePasswordAuthenticationToken auth(UUID accountId) {
		AccountPrincipal principal = new AccountPrincipal(
				com.devinolabs.uap.identity.domain.AccountId.of(accountId));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
	}

}
