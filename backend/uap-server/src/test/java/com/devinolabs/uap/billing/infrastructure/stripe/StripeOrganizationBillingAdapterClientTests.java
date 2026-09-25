package com.devinolabs.uap.billing.infrastructure.stripe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stripe.StripeClient;
import com.stripe.exception.ApiException;
import com.stripe.exception.IdempotencyException;
import com.stripe.model.Customer;
import com.stripe.model.Price;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.SubscriptionItemCollection;
import com.stripe.model.billingportal.Configuration;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;

import com.devinolabs.uap.billing.application.BillingConflictException;
import com.devinolabs.uap.billing.application.BillingProviderUnavailableException;
import com.devinolabs.uap.billing.application.InvalidWebhookSignatureException;
import com.devinolabs.uap.billing.application.OrganizationBillingProvider;
import com.devinolabs.uap.billing.domain.BillingCadence;
import com.devinolabs.uap.billing.domain.CommercialPlanKey;
import com.devinolabs.uap.billing.domain.ProviderCommercialStatus;

@ExtendWith(MockitoExtension.class)
class StripeOrganizationBillingAdapterClientTests {

	private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");
	private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

	@Mock(answer = Answers.RETURNS_DEEP_STUBS)
	private StripeClient stripeClient;

	private StripeOrganizationBillingAdapter adapter;
	private UUID organizationId;
	private UUID subscriptionId;

	@BeforeEach
	void setUp() {
		adapter = new StripeOrganizationBillingAdapter(
				stripeClient, StripeBillingPropertiesTests.validProperties(), CLOCK);
		organizationId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
		subscriptionId = UUID.fromString("11111111-2222-3333-4444-555555555555");
	}

	@Test
	void createCustomerRejectsLiveModeAndReturnsSandboxId() throws Exception {
		Customer live = new Customer();
		live.setId("cus_live");
		live.setLivemode(true);
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any())).thenReturn(live);
		assertThatThrownBy(() -> adapter.createCustomer(organizationId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sandbox");

		Customer sandbox = new Customer();
		sandbox.setId("cus_sandbox");
		sandbox.setLivemode(false);
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any())).thenReturn(sandbox);
		assertThat(adapter.createCustomer(organizationId)).isEqualTo("cus_sandbox");
	}

	@Test
	void createCheckoutSessionReturnsHostedUrl() throws Exception {
		Session session = new Session();
		session.setId("cs_test_1");
		session.setUrl("https://checkout.stripe.test/cs_test_1");
		session.setLivemode(false);
		when(stripeClient.v1().checkout().sessions().create(any(SessionCreateParams.class), any()))
				.thenReturn(session);

		OrganizationBillingProvider.CheckoutSession result = adapter.createCheckoutSession(
				organizationId, subscriptionId, "cus_sandbox", CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL);

		assertThat(result.sessionId()).isEqualTo("cs_test_1");
		assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe.test/cs_test_1");
	}

	@Test
	void fetchCheckoutSubscriptionMapsTrialingSnapshot() throws Exception {
		Session session = checkoutSession();
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(session);
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("trialing"));

		assertThat(adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.satisfies(snapshot -> {
					assertThat(snapshot.providerCustomerRef()).isEqualTo("cus_sandbox");
					assertThat(snapshot.providerSubscriptionRef()).isEqualTo("sub_test_1");
					assertThat(snapshot.status()).isEqualTo(ProviderCommercialStatus.TRIALING);
					assertThat(snapshot.cancelAtPeriodEnd()).isFalse();
					assertThat(snapshot.trialEndsAt()).isEqualTo(NOW.plusSeconds(14 * 24 * 60 * 60));
				});
	}

	@Test
	void fetchAuthoritativeSnapshotUsesSubscriptionRefWhenCheckoutSessionMissing() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("active"));
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_1",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(30),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event).status()).isEqualTo(ProviderCommercialStatus.ACTIVE);
	}

	@Test
	void verifyWebhookRejectsMissingSignature() {
		assertThatThrownBy(() -> adapter.verifyWebhook("{}".getBytes(), " "))
				.isInstanceOf(InvalidWebhookSignatureException.class);
	}

	@Test
	void stripeExceptionsSurfaceAsProviderUnavailable() throws Exception {
		when(stripeClient.v1().customers().create(any(CustomerCreateParams.class), any()))
				.thenThrow(new com.stripe.exception.ApiException("down", "req", "code", 500, null));
		assertThatThrownBy(() -> adapter.createCustomer(organizationId))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	@Test
	void createCheckoutSessionRejectsLiveMode() throws Exception {
		Session live = new Session();
		live.setId("cs_live");
		live.setUrl("https://checkout.stripe.com/cs_live");
		live.setLivemode(true);
		when(stripeClient.v1().checkout().sessions().create(any(SessionCreateParams.class), any())).thenReturn(live);

		assertThatThrownBy(() -> adapter.createCheckoutSession(
				organizationId, subscriptionId, "cus_sandbox", CommercialPlanKey.ORG_BAND_75, BillingCadence.ANNUAL))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("sandbox");
	}

	@Test
	void fetchCheckoutSubscriptionRejectsMetadataMismatchAndLiveMode() throws Exception {
		Session live = checkoutSession();
		live.setLivemode(true);
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(live);
		assertThatThrownBy(() -> adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.isInstanceOf(IllegalStateException.class);

		Session mismatched = checkoutSession();
		mismatched.setCustomer("cus_other");
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(mismatched);
		assertThatThrownBy(() -> adapter.fetchCheckoutSubscription(
				organizationId,
				subscriptionId,
				"cs_test_1",
				"cus_sandbox",
				CommercialPlanKey.ORG_BAND_75,
				BillingCadence.ANNUAL))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("customer");
	}

	@Test
	void fetchAuthoritativeSnapshotUsesCheckoutSessionWhenPresent() throws Exception {
		when(stripeClient.v1().checkout().sessions().retrieve("cs_test_1")).thenReturn(checkoutSession());
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("trialing"));
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_checkout",
				"checkout.session.completed",
				false,
				NOW.plusSeconds(10),
				"cs_test_1",
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event).providerSubscriptionRef()).isEqualTo("sub_test_1");
	}

	@Test
	void verifyWebhookRejectsInvalidSignatureAndUnsignedPayload() {
		assertThatThrownBy(() -> adapter.verifyWebhook("{}".getBytes(), "t=1,v1=deadbeef"))
				.isInstanceOf(InvalidWebhookSignatureException.class);
		assertThatThrownBy(() -> adapter.verifyWebhook(new byte[0], "sig"))
				.isInstanceOf(InvalidWebhookSignatureException.class);
	}

	@Test
	void verifyWebhookAcceptsSignedSubscriptionEvent() throws Exception {
		String payload = """
				{"id":"evt_test_1","object":"event","api_version":"2026-08-26.dahlia","created":1757764800,"type":"customer.subscription.updated","livemode":false,"data":{"object":{"id":"sub_test_1","object":"subscription","customer":"cus_sandbox","status":"active","livemode":false,"metadata":{"uap_organization_id":"%s","uap_subscription_id":"%s"}}}}
				""".formatted(organizationId, subscriptionId).strip();
		String header = StripeOrganizationBillingAdapterTests.signedWebhookHeader(
				payload, StripeBillingPropertiesTests.validProperties().getWebhookSecret());

		assertThat(adapter.verifyWebhook(payload.getBytes(), header)).satisfies(event -> {
			assertThat(event.eventId()).isEqualTo("evt_test_1");
			assertThat(event.eventType()).isEqualTo("customer.subscription.updated");
			assertThat(event.providerSubscriptionRef()).isEqualTo("sub_test_1");
			assertThat(event.organizationId()).isEqualTo(organizationId);
			assertThat(event.subscriptionId()).isEqualTo(subscriptionId);
			assertThat(event.liveMode()).isFalse();
		});
	}

	@Test
	void verifyWebhookSignedEventWithoutObjectIsProviderUnavailable() throws Exception {
		String payload = "{\"id\":\"evt_empty\"}";
		String header = StripeOrganizationBillingAdapterTests.signedWebhookHeader(
				payload, StripeBillingPropertiesTests.validProperties().getWebhookSecret());
		assertThatThrownBy(() -> adapter.verifyWebhook(payload.getBytes(), header))
				.isInstanceOf(BillingProviderUnavailableException.class);
	}

	@Test
	void priceWinsWhenMetadataNamesADifferentPlan() throws Exception {
		Subscription subscription = subscription("active");
		subscription.setMetadata(Map.of(
				"uap_organization_id", organizationId.toString(),
				"uap_subscription_id", subscriptionId.toString(),
				"uap_plan_key", CommercialPlanKey.ORG_BAND_25.name(),
				"uap_billing_cadence", BillingCadence.MONTHLY.name()));
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription);
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_price",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(10),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event)).satisfies(snapshot -> {
			assertThat(snapshot.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
			assertThat(snapshot.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
		});
	}

	@Test
	void missingIdentityMetadataStillFollowsTheItemPrice() throws Exception {
		Subscription subscription = subscription("active");
		subscription.setMetadata(Map.of(
				"plan_key", CommercialPlanKey.ORG_BAND_25.name(),
				"uap_plan_key", CommercialPlanKey.ORG_BAND_25.name(),
				"uap_billing_cadence", BillingCadence.MONTHLY.name()));
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription);
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_no_identity",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(10),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThat(adapter.fetchAuthoritativeSnapshot(event)).satisfies(snapshot -> {
			assertThat(snapshot.planKey()).isEqualTo(CommercialPlanKey.ORG_BAND_75);
			assertThat(snapshot.billingCadence()).isEqualTo(BillingCadence.ANNUAL);
		});
	}

	@Test
	void conflictingIdentityMetadataIsRejected() throws Exception {
		Subscription subscription = subscription("active");
		subscription.setMetadata(Map.of(
				"uap_organization_id", UUID.randomUUID().toString(),
				"uap_subscription_id", subscriptionId.toString()));
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription);
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_conflict_identity",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(10),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThatThrownBy(() -> adapter.fetchAuthoritativeSnapshot(event))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Organization metadata");
	}

	@Test
	void conflictingSubscriptionMetadataIsRejected() throws Exception {
		Subscription subscription = subscription("active");
		subscription.setMetadata(Map.of(
				"uap_organization_id", organizationId.toString(),
				"uap_subscription_id", UUID.randomUUID().toString()));
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription);
		OrganizationBillingProvider.VerifiedProviderEvent event = new OrganizationBillingProvider.VerifiedProviderEvent(
				"evt_conflict_subscription",
				"customer.subscription.updated",
				false,
				NOW.plusSeconds(10),
				null,
				"sub_test_1",
				organizationId,
				subscriptionId);

		assertThatThrownBy(() -> adapter.fetchAuthoritativeSnapshot(event))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Subscription metadata");
	}

	@Test
	void unknownPriceFailsClosedWithoutEchoingThePrice() throws Exception {
		Subscription subscription = subscription("active");
		subscription.getItems().getData().getFirst().getPrice().setId("price_unknown");
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription);

		assertThatThrownBy(() -> adapter.fetchSubscription("sub_test_1"))
				.isInstanceOf(BillingConflictException.class)
				.hasMessageNotContaining("price_unknown")
				.extracting(ex -> ((BillingConflictException) ex).code())
				.isEqualTo("BILLING_PROVIDER_PRICE_REJECTED");
	}

	@Test
	void planChangeUpdatesTheExistingItemWithoutResettingTrial() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("trialing"));
		ArgumentCaptor<SubscriptionUpdateParams> params = ArgumentCaptor.forClass(SubscriptionUpdateParams.class);
		ArgumentCaptor<RequestOptions> options = ArgumentCaptor.forClass(RequestOptions.class);
		when(stripeClient.v1().subscriptions().update(eq("sub_test_1"), params.capture(), options.capture()))
				.thenReturn(subscription("trialing"));
		UUID requestId = UUID.randomUUID();

		adapter.changeSubscriptionPlan(
				subscriptionId,
				"sub_test_1",
				CommercialPlanKey.ORG_BAND_250,
				BillingCadence.MONTHLY,
				requestId);

		assertThat(params.getValue().getProrationBehavior())
				.isEqualTo(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE);
		assertThat(params.getValue().getPaymentBehavior())
				.isEqualTo(SubscriptionUpdateParams.PaymentBehavior.ERROR_IF_INCOMPLETE);
		assertThat(params.getValue().getTrialFromPlan()).isNull();
		assertThat(params.getValue().getTrialEnd()).isNull();
		assertThat(params.getValue().getItems()).singleElement().satisfies(item -> {
			assertThat(item.getId()).isEqualTo("si_test_1");
			assertThat(item.getPrice()).isEqualTo("price_250_monthly");
			assertThat(item.getQuantity()).isNull();
		});
		assertThat(options.getValue().getIdempotencyKey())
				.isEqualTo("athlete-readiness:plan-change:" + subscriptionId + ":" + requestId);
	}

	@Test
	void paymentFailureThatLeavesThePriceUnchangedIsNotAProviderOutage() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("active"));
		when(stripeClient.v1().subscriptions().update(eq("sub_test_1"), any(SubscriptionUpdateParams.class), any()))
				.thenThrow(new ApiException("card", "req", "card_declined", 402, null));

		assertThatThrownBy(() -> adapter.changeSubscriptionPlan(
				subscriptionId,
				"sub_test_1",
				CommercialPlanKey.ORG_BAND_250,
				BillingCadence.MONTHLY,
				UUID.randomUUID()))
				.isInstanceOf(BillingConflictException.class)
				.extracting(ex -> ((BillingConflictException) ex).code())
				.isEqualTo("BILLING_PAYMENT_NOT_APPLIED");
	}

	@Test
	void idempotencyMismatchIsARequestConflict() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("active"));
		when(stripeClient.v1().subscriptions().update(eq("sub_test_1"), any(SubscriptionUpdateParams.class), any()))
				.thenThrow(new IdempotencyException("mismatch", "req", null, 400));

		assertThatThrownBy(() -> adapter.changeSubscriptionPlan(
				subscriptionId,
				"sub_test_1",
				CommercialPlanKey.ORG_BAND_250,
				BillingCadence.ANNUAL,
				UUID.randomUUID()))
				.isInstanceOf(BillingConflictException.class)
				.extracting(ex -> ((BillingConflictException) ex).code())
				.isEqualTo("BILLING_REQUEST_CONFLICT");
	}

	@Test
	void cancelAndReactivateSetOnlyThePeriodEndFlag() throws Exception {
		when(stripeClient.v1().subscriptions().retrieve("sub_test_1")).thenReturn(subscription("active"));
		ArgumentCaptor<SubscriptionUpdateParams> params = ArgumentCaptor.forClass(SubscriptionUpdateParams.class);
		when(stripeClient.v1().subscriptions().update(eq("sub_test_1"), params.capture(), any()))
				.thenReturn(subscription("active"));
		UUID requestId = UUID.randomUUID();

		adapter.scheduleCancelAtPeriodEnd(subscriptionId, "sub_test_1", requestId);
		assertThat(params.getValue().getCancelAtPeriodEnd()).isTrue();
		assertThat(params.getValue().getItems()).isNullOrEmpty();

		adapter.reactivateSubscription(subscriptionId, "sub_test_1", requestId);
		assertThat(params.getAllValues().get(1).getCancelAtPeriodEnd()).isFalse();
	}

	@Test
	void portalSessionUsesStoredCustomerAndRejectsSubscriptionMutation() throws Exception {
		when(stripeClient.v1().billingPortal().configurations().retrieve("bpc_test_configuration"))
				.thenReturn(portalConfiguration(false));
		com.stripe.model.billingportal.Session session = new com.stripe.model.billingportal.Session();
		session.setLivemode(false);
		session.setUrl("https://billing.stripe.test/session/secret");
		ArgumentCaptor<com.stripe.param.billingportal.SessionCreateParams> params =
				ArgumentCaptor.forClass(com.stripe.param.billingportal.SessionCreateParams.class);
		when(stripeClient.v1().billingPortal().sessions().create(params.capture())).thenReturn(session);

		assertThat(adapter.createPortalSession(organizationId, "cus_sandbox").hostedUrl())
				.isEqualTo("https://billing.stripe.test/session/secret");
		assertThat(params.getValue().getCustomer()).isEqualTo("cus_sandbox");
		assertThat(params.getValue().getConfiguration()).isEqualTo("bpc_test_configuration");
		assertThat(params.getValue().getReturnUrl()).isEqualTo("https://app.example.com/coach/billing");

		when(stripeClient.v1().billingPortal().configurations().retrieve("bpc_test_configuration"))
				.thenReturn(portalConfiguration(true));
		assertThatThrownBy(() -> adapter.createPortalSession(organizationId, "cus_sandbox"))
				.isInstanceOf(BillingProviderUnavailableException.class);
		verify(stripeClient.v1().billingPortal().sessions(), times(1))
				.create(any(com.stripe.param.billingportal.SessionCreateParams.class));
	}

	private Configuration portalConfiguration(boolean subscriptionUpdateEnabled) {
		Configuration.Features.PaymentMethodUpdate payment = new Configuration.Features.PaymentMethodUpdate();
		payment.setEnabled(true);
		Configuration.Features.InvoiceHistory invoices = new Configuration.Features.InvoiceHistory();
		invoices.setEnabled(true);
		Configuration.Features.SubscriptionUpdate subscriptionUpdate = new Configuration.Features.SubscriptionUpdate();
		subscriptionUpdate.setEnabled(subscriptionUpdateEnabled);
		Configuration.Features.SubscriptionCancel subscriptionCancel = new Configuration.Features.SubscriptionCancel();
		subscriptionCancel.setEnabled(false);
		Configuration.Features.CustomerUpdate customerUpdate = new Configuration.Features.CustomerUpdate();
		customerUpdate.setEnabled(false);
		Configuration.Features features = new Configuration.Features();
		features.setPaymentMethodUpdate(payment);
		features.setInvoiceHistory(invoices);
		features.setSubscriptionUpdate(subscriptionUpdate);
		features.setSubscriptionCancel(subscriptionCancel);
		features.setCustomerUpdate(customerUpdate);
		Configuration.LoginPage loginPage = new Configuration.LoginPage();
		loginPage.setEnabled(false);
		Configuration configuration = new Configuration();
		configuration.setLivemode(false);
		configuration.setFeatures(features);
		configuration.setLoginPage(loginPage);
		return configuration;
	}

	private Session checkoutSession() {
		Session session = new Session();
		session.setId("cs_test_1");
		session.setLivemode(false);
		session.setCustomer("cus_sandbox");
		session.setSubscription("sub_test_1");
		session.setClientReferenceId(subscriptionId.toString());
		session.setMetadata(uapMetadata());
		return session;
	}

	private Subscription subscription(String status) {
		Price price = new Price();
		price.setId("price_75_annual");
		SubscriptionItem item = new SubscriptionItem();
		item.setId("si_test_1");
		item.setPrice(price);
		item.setQuantity(1L);
		item.setCurrentPeriodEnd(NOW.plusSeconds(30L * 24 * 60 * 60).getEpochSecond());
		SubscriptionItemCollection items = new SubscriptionItemCollection();
		items.setData(List.of(item));
		Subscription subscription = new Subscription();
		subscription.setId("sub_test_1");
		subscription.setLivemode(false);
		subscription.setCustomer("cus_sandbox");
		subscription.setStatus(status);
		subscription.setCancelAtPeriodEnd(false);
		subscription.setTrialEnd(NOW.plusSeconds(14L * 24 * 60 * 60).getEpochSecond());
		subscription.setMetadata(uapMetadata());
		subscription.setItems(items);
		return subscription;
	}

	private Map<String, String> uapMetadata() {
		return Map.of(
				"uap_organization_id", organizationId.toString(),
				"uap_subscription_id", subscriptionId.toString(),
				"uap_plan_key", CommercialPlanKey.ORG_BAND_75.name(),
				"uap_billing_cadence", BillingCadence.ANNUAL.name());
	}

}
