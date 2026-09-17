package com.devinolabs.uap.billing.infrastructure.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Invoice;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;

import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;

class StripeOrganizationBillingAdapterTests {

	@Test
	void checkoutUsesLockedServerPriceTrialAndDynamicMethods() {
		UUID organizationId = UUID.randomUUID();
		UUID subscriptionId = UUID.randomUUID();
		SessionCreateParams params = StripeOrganizationBillingAdapter.checkoutParams(
				StripeBillingPropertiesTests.validProperties(),
				organizationId,
				subscriptionId,
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL);

		assertThat(params.getMode()).isEqualTo(SessionCreateParams.Mode.SUBSCRIPTION);
		assertThat(params.getCustomer()).isEqualTo("cus_sandbox");
		assertThat(params.getClientReferenceId()).isEqualTo(subscriptionId.toString());
		assertThat(params.getPaymentMethodCollection())
				.isEqualTo(SessionCreateParams.PaymentMethodCollection.ALWAYS);
		assertThat(params.getPaymentMethodTypes()).isNullOrEmpty();
		assertThat(params.getAutomaticTax()).isNull();
		assertThat(params.getLineItems()).singleElement().satisfies(item -> {
			assertThat(item.getPrice()).isEqualTo("price_75_annual");
			assertThat(item.getQuantity()).isEqualTo(1L);
		});
		assertThat(params.getSubscriptionData().getTrialPeriodDays()).isEqualTo(14L);
		assertThat(params.getIntegrationIdentifier()).matches("athlete_readiness_[a-z]{8}");
	}

	@Test
	void nativeStripeStatusesMapToProviderNeutralSignals() {
		assertThat(StripeOrganizationBillingAdapter.mapStatus("incomplete"))
				.isEqualTo(ProviderCommercialStatus.PENDING);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("trialing"))
				.isEqualTo(ProviderCommercialStatus.TRIALING);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("active"))
				.isEqualTo(ProviderCommercialStatus.ACTIVE);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("past_due"))
				.isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("unpaid"))
				.isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("paused"))
				.isEqualTo(ProviderCommercialStatus.PAYMENT_ATTENTION_REQUIRED);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("canceled"))
				.isEqualTo(ProviderCommercialStatus.ENDED);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("incomplete_expired"))
				.isEqualTo(ProviderCommercialStatus.ENDED);
		assertThat(StripeOrganizationBillingAdapter.mapStatus("future_status"))
				.isEqualTo(ProviderCommercialStatus.UNKNOWN);
	}

	@Test
	void invoiceMetadataPrefersSubscriptionDetailsOverInvoiceMetadata() {
		UUID organizationId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		UUID subscriptionId = UUID.fromString("11111111-2222-3333-4444-555555555555");
		Invoice invoice = new Invoice();
		invoice.setMetadata(java.util.Map.of("uap_organization_id", "should-not-win"));
		Invoice.Parent parent = new Invoice.Parent();
		Invoice.Parent.SubscriptionDetails details = new Invoice.Parent.SubscriptionDetails();
		details.setSubscription("sub_test");
		details.setMetadata(java.util.Map.of(
				"uap_organization_id", organizationId.toString(),
				"uap_subscription_id", subscriptionId.toString()));
		parent.setSubscriptionDetails(details);
		invoice.setParent(parent);

		assertThat(StripeOrganizationBillingAdapter.invoiceMetadata(invoice))
				.containsEntry("uap_organization_id", organizationId.toString())
				.containsEntry("uap_subscription_id", subscriptionId.toString());
	}

	@Test
	void webhookSignatureVerificationRejectsInvalidHeader() throws Exception {
		String payload = "{\"id\":\"evt_test\"}";
		String secret = "whsec_placeholder_not_a_real_secret";
		assertThatThrownBy(() -> Webhook.constructEvent(payload, "t=1,v1=deadbeef", secret))
				.isInstanceOf(SignatureVerificationException.class);

		long timestamp = Instant.now().getEpochSecond();
		String header = signedHeader(payload, secret, timestamp);
		assertThatThrownBy(() -> Webhook.constructEvent(payload, header, "whsec_other_secret"))
				.isInstanceOf(SignatureVerificationException.class);
	}

	private static String signedHeader(String payload, String secret, long timestamp)
			throws NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		String hex = HexFormat.of().formatHex(mac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
		return "t=" + timestamp + ",v1=" + hex;
	}

}
