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

import com.devinolabs.uap.billing.application.GooglePlayPurchaseValidationService;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.BillingProvider;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.IndividualManagementChannel;
import com.devinolabs.uap.billing.domain.SubscriptionLifecycleState;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class GooglePlayBillingControllerTests {

	@Mock
	private GooglePlayPurchaseValidationService purchaseValidationService;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
		validator.afterPropertiesSet();
		mockMvc = MockMvcBuilders.standaloneSetup(new GooglePlayBillingController(purchaseValidationService))
				.setControllerAdvice(new BillingExceptionHandler())
				.setValidator(validator)
				.setMessageConverters(new JacksonJsonHttpMessageConverter(JsonMapper.builder().build()))
				.build();
	}

	@Test
	void validateRequiresAuthenticatedAccountPrincipal() throws Exception {
		assertThatThrownBy(() -> mockMvc.perform(post("/api/v1/billing/account/google-play/purchases")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"purchaseToken":"google-play-purchase-token-fixture-001","productId":"premium.monthly"}
								"""))
				.andReturn())
				.isInstanceOf(ServletException.class)
				.hasCauseInstanceOf(IllegalStateException.class)
				.cause()
				.hasMessageContaining("Authenticated AccountPrincipal is required");

		verify(purchaseValidationService, never()).validateOrRestore(any(), any(), any());
	}

	@Test
	void validateRejectsBlankPurchaseToken() throws Exception {
		mockMvc.perform(post("/api/v1/billing/account/google-play/purchases")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"purchaseToken\":\"\",\"productId\":\"premium.monthly\"}")
						.principal(auth(UUID.randomUUID())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

		verify(purchaseValidationService, never()).validateOrRestore(any(), any(), any());
	}

	@Test
	void validateReturnsSubscriptionForAuthenticatedAccount() throws Exception {
		UUID accountId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		when(purchaseValidationService.validateOrRestore(
						eq(accountId),
						eq("google-play-purchase-token-fixture-001"),
						eq("premium.monthly")))
				.thenReturn(new GooglePlayPurchaseValidationService.SubscriptionResult(
						subscriptionId,
						BillingProvider.GOOGLE_PLAY,
						IndividualManagementChannel.GOOGLE_PLAY,
						CommercialPlanKey.INDIVIDUAL_PREMIUM,
						BillingCadence.MONTHLY,
						SubscriptionLifecycleState.ACTIVE,
						null,
						Instant.parse("2026-10-01T00:00:00Z"),
						null));

		mockMvc.perform(post("/api/v1/billing/account/google-play/purchases")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"purchaseToken":"google-play-purchase-token-fixture-001","productId":"premium.monthly"}
								""")
						.principal(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
				.andExpect(jsonPath("$.provider").value("GOOGLE_PLAY"))
				.andExpect(jsonPath("$.managementChannel").value("GOOGLE_PLAY"))
				.andExpect(jsonPath("$.lifecycleState").value("ACTIVE"));
	}

	@Test
	void invalidPurchaseMapsToBadRequest() throws Exception {
		UUID accountId = UUID.randomUUID();
		when(purchaseValidationService.validateOrRestore(eq(accountId), any(), any()))
				.thenThrow(new InvalidGooglePlayPurchaseException("rejected"));

		mockMvc.perform(post("/api/v1/billing/account/google-play/purchases")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"purchaseToken":"google-play-purchase-token-fixture-001","productId":"premium.monthly"}
								""")
						.principal(auth(accountId)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("BILLING_GOOGLE_PLAY_PURCHASE_REJECTED"));
	}

	private static UsernamePasswordAuthenticationToken auth(UUID accountId) {
		AccountPrincipal principal = new AccountPrincipal(
				com.devinolabs.uap.identity.domain.AccountId.of(accountId));
		return UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.authorities());
	}

}
